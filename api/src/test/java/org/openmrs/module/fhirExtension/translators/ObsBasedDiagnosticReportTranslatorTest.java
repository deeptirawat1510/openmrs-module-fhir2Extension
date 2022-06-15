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
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.fhir2.api.translators.DiagnosticReportTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsLabResultTranslatorImpl;
import org.openmrs.module.fhirExtension.translators.impl.ObsBasedDiagnosticReportTranslatorImpl;

import java.util.Date;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ObsBasedDiagnosticReportTranslatorTest {
	
	private static final String REPORT_URL = "/100/uploadReport.pdf";
	
	private static final String REPORT_NAME = "bloodTest.pdf";
	
	private static final String LAB_TEST_NOTES = "Report is normal";
	
	@Mock
	private DiagnosticReportTranslator diagnosticReportTranslator;
	
	@Mock
	private DiagnosticReportObsLabResultTranslatorImpl diagnosticReportObsLabResultTranslator;
	
	@InjectMocks
	private final ObsBasedDiagnosticReportTranslator translator = new ObsBasedDiagnosticReportTranslatorImpl();
	
	@Captor
	ArgumentCaptor<LabResult> labResultArgumentCaptor = ArgumentCaptor.forClass(LabResult.class);
	
	private final Patient patient = new Patient();
	
	private final Date issuedDate = new Date();
	
	@Test
	public void shouldTranslateDiagnosticReportToOpenMrsTypeToUpdateObsWithReportUrlAndNameWhenReportFileIsAttached() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		Attachment attachment = new Attachment();
		attachment.setUrl(REPORT_URL);
		attachment.setTitle(REPORT_NAME);
		diagnosticReport.setPresentedForm(singletonList(attachment));
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setIssued(issuedDate);
		when(diagnosticReportTranslator.toOpenmrsType(diagnosticReport)).thenReturn(fhirDiagnosticReport);
		Obs obs = new Obs();
		when(diagnosticReportObsLabResultTranslator.toOpenmrsType(any(LabResult.class))).thenReturn(obs);
		
		FhirDiagnosticReport result = translator.toOpenmrsType(diagnosticReport);
		
		assertEquals(fhirDiagnosticReport, result);
		assertEquals(obs, result.getResults().iterator().next());
		verify(diagnosticReportObsLabResultTranslator).toOpenmrsType(labResultArgumentCaptor.capture());
		LabResult labResult = labResultArgumentCaptor.getValue();
		assertEquals(REPORT_URL, labResult.getLabReportUrl());
		assertEquals(REPORT_NAME, labResult.getLabReportFileName());
		assertNull(labResult.getLabReportNotes());
		Concept testConcept = newConcept();
		BiFunction<Concept, String, Obs> obsFactory = labResult.getObsFactory();
		Obs resultObs = obsFactory.apply(testConcept, "result");
		assertEquals(patient, resultObs.getPerson());
		assertEquals(issuedDate, resultObs.getObsDatetime());
		assertEquals(testConcept, resultObs.getConcept());
		assertEquals("result", resultObs.getValueText());
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
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setIssued(issuedDate);
		when(diagnosticReportTranslator.toOpenmrsType(diagnosticReport)).thenReturn(fhirDiagnosticReport);
		Obs obs = new Obs();
		when(diagnosticReportObsLabResultTranslator.toOpenmrsType(any(LabResult.class))).thenReturn(obs);
		
		FhirDiagnosticReport result = translator.toOpenmrsType(diagnosticReport);
		
		assertEquals(fhirDiagnosticReport, result);
		assertEquals(obs, result.getResults().iterator().next());
		verify(diagnosticReportObsLabResultTranslator).toOpenmrsType(labResultArgumentCaptor.capture());
		LabResult labResult = labResultArgumentCaptor.getValue();
		assertEquals(REPORT_URL, labResult.getLabReportUrl());
		assertEquals(REPORT_NAME, labResult.getLabReportFileName());
		assertEquals(LAB_TEST_NOTES, labResult.getLabReportNotes());
		Concept testConcept = newConcept();
		BiFunction<Concept, String, Obs> obsFactory = labResult.getObsFactory();
		Obs resultObs = obsFactory.apply(testConcept, "result");
		assertEquals(patient, resultObs.getPerson());
		assertEquals(issuedDate, resultObs.getObsDatetime());
		assertEquals(testConcept, resultObs.getConcept());
		assertEquals("result", resultObs.getValueText());
	}
	
	@Test
	public void shouldTranslateDiagnosticReportToOpenMrsTypeToNotUpdateObsWhenOnlyConclusionIsEntered() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		diagnosticReport.setConclusion(LAB_TEST_NOTES);
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		when(diagnosticReportTranslator.toOpenmrsType(diagnosticReport)).thenReturn(fhirDiagnosticReport);
		
		FhirDiagnosticReport result = translator.toOpenmrsType(diagnosticReport);
		
		assertEquals(fhirDiagnosticReport, result);
		assertTrue(result.getResults().isEmpty());
		verify(diagnosticReportObsLabResultTranslator, times(0)).toOpenmrsType(any(LabResult.class));
	}
	
	@Test
	public void shouldTranslateFhirDiagnosticReportToFhirTypeToUpdatePresentedFormAndConclusionWhenItIsPresentInTheObs() {
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		Obs obs = new Obs();
		fhirDiagnosticReport.setResults(of(obs).collect(toSet()));
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		when(diagnosticReportTranslator.toFhirResource(fhirDiagnosticReport)).thenReturn(diagnosticReport);
		LabResult labResult = LabResult.builder().labReportUrl(REPORT_URL).labReportNotes(LAB_TEST_NOTES).build();
		when(diagnosticReportObsLabResultTranslator.toFhirResource(obs)).thenReturn(labResult);
		
		DiagnosticReport result = translator.toFhirResource(fhirDiagnosticReport);
		
		assertEquals(diagnosticReport, result);
		assertTrue(diagnosticReport.hasPresentedForm());
		assertEquals(REPORT_URL, result.getPresentedForm().get(0).getUrl());
		assertEquals(LAB_TEST_NOTES, diagnosticReport.getConclusion());
	}
	
	@Test
	public void shouldTranslateFhirDiagnosticReportToFhirTypeToNotUpdatePresentedFormWhenObsResultIsNotPresent() {
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		fhirDiagnosticReport.setResults(new HashSet<>());
		when(diagnosticReportTranslator.toFhirResource(fhirDiagnosticReport)).thenReturn(diagnosticReport);

		DiagnosticReport result = translator.toFhirResource(fhirDiagnosticReport);

		assertEquals(diagnosticReport, result);
		assertFalse(diagnosticReport.hasPresentedForm());
		verify(diagnosticReportObsLabResultTranslator, times(0)).toFhirResource(any());
	}
	
	@Test
	public void shouldTranslateFhirDiagnosticReportToFhirTypeToNotUpdatePresentedFormWhenLabReportIsNotPresentInObsResult() {
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		Obs obs = new Obs();
		fhirDiagnosticReport.setResults(of(obs).collect(toSet()));
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		when(diagnosticReportTranslator.toFhirResource(fhirDiagnosticReport)).thenReturn(diagnosticReport);
		LabResult labResult = LabResult.builder().labReportNotes(LAB_TEST_NOTES).build();
		when(diagnosticReportObsLabResultTranslator.toFhirResource(obs)).thenReturn(labResult);
		
		DiagnosticReport result = translator.toFhirResource(fhirDiagnosticReport);
		
		assertEquals(diagnosticReport, result);
		assertFalse(diagnosticReport.hasPresentedForm());
	}
	
	private Concept newConcept() {
		Concept concept = new Concept();
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setHl7Abbreviation(ConceptDatatype.TEXT);
		concept.setDatatype(conceptDatatype);
		return concept;
	}
}
