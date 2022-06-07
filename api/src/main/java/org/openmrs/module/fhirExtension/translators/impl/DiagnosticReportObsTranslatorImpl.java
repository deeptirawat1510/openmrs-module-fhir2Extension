package org.openmrs.module.fhirExtension.translators.impl;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.api.translators.DiagnosticReportTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.DiagnosticReportObsTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import static java.util.Collections.singletonList;

@Component
public class DiagnosticReportObsTranslatorImpl implements DiagnosticReportObsTranslator {

	@Autowired
	private DiagnosticReportTranslator diagnosticReportTranslator;

	@Autowired
	private DiagnosticReportObsTranslatorHelper diagnosticReportObsTranslatorHelper;

	@Autowired
	private ConceptService conceptService;
	
	@Override
	public DiagnosticReport toFhirResource(@Nonnull FhirDiagnosticReport fhirDiagnosticReport) {
		DiagnosticReport diagnosticReport = diagnosticReportTranslator.toFhirResource(fhirDiagnosticReport);
		createPresentedForm(diagnosticReport, fhirDiagnosticReport);
		return diagnosticReport;
	}

	@Override
	public FhirDiagnosticReport toOpenmrsType(@Nonnull DiagnosticReport diagnosticReport) {
		FhirDiagnosticReport fhirDiagnosticReport = diagnosticReportTranslator.toOpenmrsType(diagnosticReport);
		updateObsResults(fhirDiagnosticReport, diagnosticReport);
		return fhirDiagnosticReport;
	}

	private void updateObsResults(FhirDiagnosticReport fhirDiagnosticReport, DiagnosticReport diagnosticReport) {
		if(diagnosticReport.hasPresentedForm()){
			Attachment labAttachment = diagnosticReport.getPresentedForm().get(0);
			LabResult labResult = new LabResult();
			labResult.setPatient(fhirDiagnosticReport.getSubject());
			labResult.setObservationDate(fhirDiagnosticReport.getIssued());
			labResult.setConcept(fhirDiagnosticReport.getCode());
			labResult.setLabReportUrl(labAttachment.getUrl());
			labResult.setLabReportNotes(diagnosticReport.getConclusion());
			labResult.setLabReportFileName(labAttachment.getTitle());
			fhirDiagnosticReport.setResults(new HashSet<>(
					singletonList(diagnosticReportObsTranslatorHelper.createObs(labResult))));
		}
	}

	private void createPresentedForm(DiagnosticReport diagnosticReport, FhirDiagnosticReport fhirDiagnosticReport) {
		Iterator<Obs> obsResultsIterator = fhirDiagnosticReport.getResults().iterator();
		if(obsResultsIterator.hasNext()){
			Obs obsResult = obsResultsIterator.next();
			LabResult labResult = diagnosticReportObsTranslatorHelper.getLabResult(obsResult);
			if (labResult.isLabReportPresent()) {
				Attachment attachment = new Attachment();
				attachment.setUrl(labResult.getLabReportUrl());
				attachment.setTitle(labResult.getLabReportFileName());
				diagnosticReport.setPresentedForm(Arrays.asList(attachment));
			}
			diagnosticReport.setConclusion(labResult.getLabReportNotes());
		}
	}
}