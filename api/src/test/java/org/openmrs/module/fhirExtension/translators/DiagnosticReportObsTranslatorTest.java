package org.openmrs.module.fhirExtension.translators;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.translators.DiagnosticReportTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsTranslatorHelper;
import org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsTranslatorImpl;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticReportObsTranslatorTest {
	
	private static final String REPORT_URL = "/100/uploadReport.pdf";
	
	private static final String REPORT_NAME = "bloodTest.pdf";
	
	private static final String LAB_TEST_NOTES = "Report is normal";
	
	@Mock
	private DiagnosticReportTranslator diagnosticReportTranslator;

	@Mock
	private DiagnosticReportObsTranslatorHelper diagnosticReportObsTranslatorHelper;
	
	@InjectMocks
	private final DiagnosticReportObsTranslator diagnosticReportObsTranslatorImpl = new DiagnosticReportObsTranslatorImpl();

	@Captor
	ArgumentCaptor<LabResult> labResultArgumentCaptor = ArgumentCaptor.forClass(LabResult.class);

	@Test
	public void shouldTranslateDiagnosticReportToOpenMrsTypeToUpdateObsWithReportUrlAndNameWhenReportFileIsAttached() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		Attachment attachment = new Attachment();
		attachment.setUrl(REPORT_URL);
		attachment.setTitle(REPORT_NAME);
		diagnosticReport.setPresentedForm(singletonList(attachment));
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		when(diagnosticReportTranslator.toOpenmrsType(diagnosticReport)).thenReturn(fhirDiagnosticReport);
		Obs obs = new Obs();
		when(diagnosticReportObsTranslatorHelper.createObs(any(LabResult.class))).thenReturn(obs);

		FhirDiagnosticReport result = diagnosticReportObsTranslatorImpl.toOpenmrsType(diagnosticReport);
		
		assertEquals(fhirDiagnosticReport, result);
		assertEquals(obs, result.getResults().iterator().next());
		verify(diagnosticReportObsTranslatorHelper).createObs(labResultArgumentCaptor.capture());
		LabResult labResult = labResultArgumentCaptor.getValue();
		assertEquals(REPORT_URL, labResult.getLabReportUrl());
		assertEquals(REPORT_NAME, labResult.getLabReportFileName());
		assertNull(labResult.getLabReportNotes());
	}
	
	@Test
	public void shouldTranslateDiagnosticReportToOpenMrsTypeToUpdateObsWithReportUrlNameAndNotesWhenReportFileIsAttachedAndConclusionIsEntered() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		Attachment attachment = new Attachment();
		attachment.setUrl(REPORT_URL);
		attachment.setTitle(REPORT_NAME);
		diagnosticReport.setPresentedForm(singletonList(attachment));
		diagnosticReport.setConclusion(LAB_TEST_NOTES);
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		when(diagnosticReportTranslator.toOpenmrsType(diagnosticReport)).thenReturn(fhirDiagnosticReport);
		Obs obs = new Obs();
		when(diagnosticReportObsTranslatorHelper.createObs(any(LabResult.class))).thenReturn(obs);

		FhirDiagnosticReport result = diagnosticReportObsTranslatorImpl.toOpenmrsType(diagnosticReport);

		assertEquals(fhirDiagnosticReport, result);
		assertEquals(obs, result.getResults().iterator().next());
		verify(diagnosticReportObsTranslatorHelper).createObs(labResultArgumentCaptor.capture());
		LabResult labResult = labResultArgumentCaptor.getValue();
		assertEquals(REPORT_URL, labResult.getLabReportUrl());
		assertEquals(REPORT_NAME, labResult.getLabReportFileName());
		assertEquals(LAB_TEST_NOTES, labResult.getLabReportNotes());
	}

	@Test
	public void shouldTranslateDiagnosticReportToOpenMrsTypeToNotUpdateObsWhenOnlyConclusionIsEntered() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		diagnosticReport.setConclusion(LAB_TEST_NOTES);
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		when(diagnosticReportTranslator.toOpenmrsType(diagnosticReport)).thenReturn(fhirDiagnosticReport);

		FhirDiagnosticReport result = diagnosticReportObsTranslatorImpl.toOpenmrsType(diagnosticReport);

		assertEquals(fhirDiagnosticReport, result);
		assertTrue(result.getResults().isEmpty());
		verify(diagnosticReportObsTranslatorHelper, times(0)).createObs(any(LabResult.class));
	}
}