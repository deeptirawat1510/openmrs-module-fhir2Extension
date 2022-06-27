package org.openmrs.module.fhirExtension.translators.impl;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.module.fhir2.api.translators.DiagnosticReportTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.ObsBasedDiagnosticReportTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.stream.Stream.of;

@Component
public class ObsBasedDiagnosticReportTranslatorImpl implements ObsBasedDiagnosticReportTranslator {
	
	@Autowired
	private DiagnosticReportTranslator diagnosticReportTranslator;
	
	@Autowired
	private DiagnosticReportObsLabResultTranslatorImpl diagnosticReportObsLabResultTranslator;
	
	@Override
	public DiagnosticReport toFhirResource(@Nonnull FhirDiagnosticReport fhirDiagnosticReport) {
		DiagnosticReport diagnosticReport = diagnosticReportTranslator.toFhirResource(fhirDiagnosticReport);
		setPresentedForm(diagnosticReport, fhirDiagnosticReport);
		return diagnosticReport;
	}
	
	@Override
	public FhirDiagnosticReport toOpenmrsType(@Nonnull DiagnosticReport diagnosticReport) {
		FhirDiagnosticReport fhirDiagnosticReport = diagnosticReportTranslator.toOpenmrsType(diagnosticReport);
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
	
	private BiFunction<Concept, String, Obs> newObs(Patient subject, Date issued) {
		return (concept, value) -> {
			Obs obs = new Obs();
			obs.setPerson(subject);
			obs.setObsDatetime(issued);
			obs.setConcept(concept);
			setObsValue(obs, value);
			return obs;
		};
	}
	
	private void setObsValue(Obs obs, String value) {
		if (value != null) {
			try {
				obs.setValueAsString(value);
			}
			catch (ParseException e) {
				throw new APIException(e);
			}
		}
	}
}
