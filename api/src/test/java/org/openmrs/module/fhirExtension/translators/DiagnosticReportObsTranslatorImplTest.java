package org.openmrs.module.fhirExtension.translators;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.springframework.util.CollectionUtils;

import java.util.Set;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticReportObsTranslatorImplTest {
	
	private static final String CODE = "249b9094-5083-454c-a4ec-9a4babf26558";
	
	private static final String REPORT_URL = "/100/uploadReport.pdf";
	
	private static final String REPORT_NAME = "bloodTest.pdf";
	
	private static final String LAB_TEST_NOTES = "Report is normal";
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private ObservationReferenceTranslator observationReferenceTranslator;
	
	@InjectMocks
	private final DiagnosticReportObsTranslator diagnosticReportObsTranslator = new DiagnosticReportObsTranslator();
	
	@Test
	public void shouldTranslateDiagnosticReportToOpenMrsTypeWhenOnlyPatientStatusAndCodeIsPresent() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
		Reference subjectReference = new Reference();
		subjectReference.setType(FhirConstants.PATIENT);
		diagnosticReport.setSubject(subjectReference);
		when(patientReferenceTranslator.toOpenmrsType(subjectReference)).thenReturn(new Patient());
		CodeableConcept code = new CodeableConcept();
		code.addCoding().setCode(CODE);
		diagnosticReport.setCode(code);
		when(conceptTranslator.toOpenmrsType(code)).thenReturn(new Concept());
		
		FhirDiagnosticReport fhirDiagnosticReport = diagnosticReportObsTranslator.toOpenmrsType(diagnosticReport);
		
		assertNotNull(fhirDiagnosticReport);
		assertEquals(FhirDiagnosticReport.DiagnosticReportStatus.FINAL, fhirDiagnosticReport.getStatus());
		assertNotNull(fhirDiagnosticReport.getSubject());
		assertNotNull(fhirDiagnosticReport.getCode());
		assertNotNull(fhirDiagnosticReport.getIssued());
		assertTrue(CollectionUtils.isEmpty(fhirDiagnosticReport.getResults()));
	}
	
	@Test
	public void shouldTranslateDiagnosticReportToOpenMrsTypeToUpdateObsWithReportUrlAndNameWhenReportFileIsAttached() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		Attachment attachment = new Attachment();
		attachment.setUrl(REPORT_URL);
		attachment.setTitle(REPORT_NAME);
		diagnosticReport.setPresentedForm(singletonList(attachment));
		Concept concept = createConcept();
		when(conceptService.getConceptByName("LAB_REPORT")).thenReturn(concept);
		when(conceptService.getConceptByName("LAB_RESULT")).thenReturn(concept);
		
		FhirDiagnosticReport fhirDiagnosticReport = diagnosticReportObsTranslator.toOpenmrsType(diagnosticReport);
		
		assertNotNull(fhirDiagnosticReport);
		Set<Obs> obsTopLevel = fhirDiagnosticReport.getResults();
		assertEquals(1, obsTopLevel.size());
		Obs obsSecondLevel = obsTopLevel.iterator().next();
		assertEquals(1, obsSecondLevel.getGroupMembers().size());
		Obs obsThirdLevel = obsSecondLevel.getGroupMembers().iterator().next();
		assertEquals(2, obsThirdLevel.getGroupMembers().size());
	}
	
	@Test
	public void shouldTranslateDiagnosticReportToOpenMrsTypeToUpdateObsWithReportUrlNameAndNotesWhenReportFileIsAttachedAndConclusionIsEntered() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		Attachment attachment = new Attachment();
		attachment.setUrl(REPORT_URL);
		attachment.setTitle(REPORT_NAME);
		diagnosticReport.setPresentedForm(singletonList(attachment));
		diagnosticReport.setConclusion(LAB_TEST_NOTES);
		Concept concept = createConcept();
		when(conceptService.getConceptByName("LAB_REPORT")).thenReturn(concept);
		when(conceptService.getConceptByName("LAB_RESULT")).thenReturn(concept);
		when(conceptService.getConceptByName("LAB_NOTES")).thenReturn(concept);
		
		FhirDiagnosticReport fhirDiagnosticReport = diagnosticReportObsTranslator.toOpenmrsType(diagnosticReport);
		
		assertNotNull(fhirDiagnosticReport);
		Set<Obs> obsTopLevel = fhirDiagnosticReport.getResults();
		assertEquals(1, obsTopLevel.size());
		Obs obsSecondLevel = obsTopLevel.iterator().next();
		assertEquals(1, obsSecondLevel.getGroupMembers().size());
		Obs obsThirdLevel = obsSecondLevel.getGroupMembers().iterator().next();
		assertEquals(3, obsThirdLevel.getGroupMembers().size());
	}
	
	@Test
	public void shouldTranslateDiagnosticReportToOpenMrsTypeToUpdateObsWithNotesWhenConclusionIsEntered() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		diagnosticReport.setConclusion(LAB_TEST_NOTES);
		Concept concept = createConcept();
		when(conceptService.getConceptByName("LAB_NOTES")).thenReturn(concept);
		
		FhirDiagnosticReport fhirDiagnosticReport = diagnosticReportObsTranslator.toOpenmrsType(diagnosticReport);
		
		assertNotNull(fhirDiagnosticReport);
		assertTrue(CollectionUtils.isEmpty(fhirDiagnosticReport.getResults()));
	}
	
	private Concept createConcept() {
		Concept concept = new Concept();
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setHl7Abbreviation(ConceptDatatype.TEXT);
		concept.setDatatype(conceptDatatype);
		return concept;
	}
	
}
