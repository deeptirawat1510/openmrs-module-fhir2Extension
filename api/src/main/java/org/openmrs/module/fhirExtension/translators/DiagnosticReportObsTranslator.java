package org.openmrs.module.fhirExtension.translators;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.api.translators.impl.DiagnosticReportTranslatorImpl;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;

@Primary
@Component
public class DiagnosticReportObsTranslator extends DiagnosticReportTranslatorImpl {
	
	@Autowired
	private ConceptService conceptService;
	
	@Override
	public FhirDiagnosticReport toOpenmrsType(@Nonnull FhirDiagnosticReport fhirDiagnosticReport,
	        @Nonnull DiagnosticReport diagnosticReport) {
		super.toOpenmrsType(fhirDiagnosticReport, diagnosticReport);
		updateResults(fhirDiagnosticReport, diagnosticReport);
		return fhirDiagnosticReport;
	}
	
	private void updateResults(FhirDiagnosticReport fhirDiagnosticReport, DiagnosticReport diagnosticReport) {
		if(diagnosticReport.hasPresentedForm()){
			Attachment labAttachment = diagnosticReport.getPresentedForm().get(0);
			Obs obs = LabTest.builder()
					.patient(fhirDiagnosticReport.getSubject())
					.observationDate(fhirDiagnosticReport.getIssued())
					.testConcept(fhirDiagnosticReport.getCode())
					.labReportNotes(diagnosticReport.getConclusion())
					.labReportUrl(labAttachment.getUrl())
					.labReportName(labAttachment.getTitle())
					.conceptFunction(conceptName -> conceptService.getConceptByName(conceptName))
					.build().toObsModel();
			fhirDiagnosticReport.setResults(new HashSet<>(Collections.singletonList(obs)));
		}
	}
}
