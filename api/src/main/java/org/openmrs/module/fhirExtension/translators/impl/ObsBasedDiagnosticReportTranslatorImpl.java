package org.openmrs.module.fhirExtension.translators.impl;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.module.fhir2.api.translators.DiagnosticReportTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.ObsBasedDiagnosticReportTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.stream.Stream.of;

@Component
public class ObsBasedDiagnosticReportTranslatorImpl implements ObsBasedDiagnosticReportTranslator {
	
	@Autowired
	private DiagnosticReportTranslator diagnosticReportTranslator;
	
	@Autowired
	private DiagnosticReportObsLabResultTranslatorImpl diagnosticReportObsLabResultTranslator;
	
	@Autowired
	private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	@Override
	public DiagnosticReport toFhirResource(@Nonnull FhirDiagnosticReport fhirDiagnosticReport) {
		DiagnosticReport diagnosticReport = diagnosticReportTranslator.toFhirResource(fhirDiagnosticReport);
		setPresentedForm(diagnosticReport, fhirDiagnosticReport);
		setPractitioner(diagnosticReport, fhirDiagnosticReport);
		return diagnosticReport;
	}
	
	@Override
	public FhirDiagnosticReport toOpenmrsType(@Nonnull DiagnosticReport diagnosticReport) {
		FhirDiagnosticReport fhirDiagnosticReport = diagnosticReportTranslator.toOpenmrsType(diagnosticReport);
		setPerformer(diagnosticReport, fhirDiagnosticReport);
		updateObsResults(fhirDiagnosticReport, diagnosticReport);
		return fhirDiagnosticReport;
	}
	
	private void updateObsResults(FhirDiagnosticReport fhirDiagnosticReport, DiagnosticReport diagnosticReport) {
		if (diagnosticReport.hasPresentedForm()) {
			Attachment labAttachment = diagnosticReport.getPresentedForm().get(0);
			LabResult labResult = LabResult.builder()
					.labReportUrl(labAttachment.getUrl())
					.labReportNotes(diagnosticReport.getConclusion())
					.labReportFileName(labAttachment.getTitle())
					.concept(fhirDiagnosticReport.getCode())
					.obsFactory(newObs(fhirDiagnosticReport.getSubject(), fhirDiagnosticReport.getIssued()))
					.build();

			fhirDiagnosticReport.setResults(
					of(diagnosticReportObsLabResultTranslator.toOpenmrsType(labResult))
							.filter(Objects::nonNull)
							.collect(Collectors.toSet()));
		}
	}
	
	private void setPresentedForm(DiagnosticReport diagnosticReport, FhirDiagnosticReport fhirDiagnosticReport) {
		Iterator<Obs> obsResultsIterator = fhirDiagnosticReport.getResults().iterator();
		if (obsResultsIterator.hasNext()) {
			Obs obsResult = obsResultsIterator.next();
			LabResult labResult = diagnosticReportObsLabResultTranslator.toFhirResource(obsResult);
			if (labResult.isLabReportPresent()) {
				Attachment attachment = new Attachment();
				attachment.setUrl(labResult.getLabReportUrl());
				attachment.setTitle(labResult.getLabReportFileName());
				diagnosticReport.addPresentedForm(attachment);
			}
			diagnosticReport.setConclusion(labResult.getLabReportNotes());
		}
	}
	
	private void setPractitioner(DiagnosticReport diagnosticReport, FhirDiagnosticReport fhirDiagnosticReport) {
		Set<Provider> performers = fhirDiagnosticReport.getPerformers();
		if (!performers.isEmpty()) {
			Provider doctorProvider = performers.iterator().next();
			Reference reference = practitionerReferenceTranslator.toFhirResource(doctorProvider);
			diagnosticReport.setPerformer(Collections.singletonList(reference));
		}
	}
	
	private void setPerformer(DiagnosticReport diagnosticReport, FhirDiagnosticReport fhirDiagnosticReport) {
		if (!diagnosticReport.getPerformer().isEmpty()) {
			Provider provider = practitionerReferenceTranslator.toOpenmrsType(diagnosticReport.getPerformer().get(0));
			fhirDiagnosticReport.setPerformers(Collections.singleton(provider));
		}
	}
	
	private BiFunction<Concept, Object, Obs> newObs(Patient subject, Date issued) {
		return (concept, value) -> {
			Obs obs = new Obs();
			obs.setPerson(subject);
			obs.setObsDatetime(issued);
			obs.setConcept(concept);
			setObsValue(obs, value);
			return obs;
		};
	}
	
	private void setObsValue(Obs obs, Object value) {
		if (value != null) {
			if (value instanceof Concept)
				obs.setValueCoded((Concept) value);
			else if (value instanceof Boolean) {
				obs.setValueBoolean((Boolean) value);
			} else if (value instanceof Date) {
				obs.setValueDatetime((Date) value);
			} else if (value instanceof Double) {
				obs.setValueNumeric((Double) value);
			} else {
				try {
					
					obs.setValueAsString((String) value);
				}
				catch (ParseException e) {
					throw new APIException(e);
				}
			}
		}
	}
}
