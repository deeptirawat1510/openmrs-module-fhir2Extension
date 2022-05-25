package org.openmrs.module.fhirExtension.domain;

import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.fhirExtension.domain.observation.LabTest;

import java.util.Date;
import java.util.Set;
import java.util.function.Function;

import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LabTestTest {
	
	private static final String REPORT_URL = "/100/uploadReport.pdf";
	
	private static final String REPORT_NAME = "bloodTest.pdf";
	
	private static final String LAB_TEST_NOTES = "Report is normal";
	
	private static final int PATIENT_ID = 12345;
	
	@Test
    public void shouldCreateObsModelWhenTestReportIsUploadedWithLabNotesForSingleTestWithoutAnyOrder(){
        Concept testConcept = new Concept();
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Date testDate = new Date();
        LabTest testObservation = LabTest.builder()
                .patient(patient)
                .observationDate(testDate)
                .testConcept(testConcept)
                .labReportUrl(REPORT_URL)
                .labReportNotes(LAB_TEST_NOTES)
                .labReportName(REPORT_NAME)
                .conceptFunction(getConceptFunction()).build();

        Obs obsModel = testObservation.toObsModel();

        assertPatientAndObservationDate(testDate, obsModel);
        assertEquals(testConcept, obsModel.getConcept());
        Set<Obs> obsGroupMembersTopLevel = obsModel.getGroupMembers();
        assertEquals(1, obsGroupMembersTopLevel.size());

        Obs obsModelSecondLevel = obsGroupMembersTopLevel.iterator().next();
        assertPatientAndObservationDate(testDate, obsModelSecondLevel);
        assertEquals(testConcept, obsModelSecondLevel.getConcept());
        Set<Obs> obsGroupMembersSecondLevel = obsModelSecondLevel.getGroupMembers();
        assertEquals(3, obsGroupMembersSecondLevel.size());

        obsGroupMembersSecondLevel.forEach(obsModelChildLevel -> {
            assertPatientAndObservationDate(testDate, obsModelChildLevel);
            String obsChildConceptName = obsModelChildLevel.getConcept().getPreferredName(ENGLISH).getName();
			switch (obsChildConceptName) {
				case "LAB_REPORT":
					assertEquals(REPORT_URL, obsModelChildLevel.getValueText());
					break;
				case "LAB_NOTES":
					assertEquals(LAB_TEST_NOTES, obsModelChildLevel.getValueText());
					break;
				case "LAB_RESULT":
					assertEquals(REPORT_NAME, obsModelChildLevel.getValueText());
					break;
			}
        });

    }
	
	@Test
	public void shouldNotCreateObsModelWhenBothTestReportAndNotesAreNotPresentForSingleTestWithoutAnyOrder() {
		Concept testConcept = new Concept();
		Patient patient = new Patient();
		patient.setId(PATIENT_ID);
		Date testDate = new Date();
		LabTest testObservation = LabTest.builder().patient(patient).observationDate(testDate).testConcept(testConcept)
		        .conceptFunction(getConceptFunction()).build();
		
		Obs obsModel = testObservation.toObsModel();
		
		assertNull(obsModel);
	}
	
	@Test
	public void shouldCreateObsModelWhenOnlyLabNotesAreAvailableForSingleTestWithoutAnyOrder() {
		Concept testConcept = new Concept();
		Patient patient = new Patient();
		patient.setId(PATIENT_ID);
		Date testDate = new Date();
		LabTest testObservation = LabTest.builder().patient(patient).observationDate(testDate).testConcept(testConcept)
		        .labReportNotes(LAB_TEST_NOTES).conceptFunction(getConceptFunction()).build();
		
		Obs obsModel = testObservation.toObsModel();
		
		assertPatientAndObservationDate(testDate, obsModel);
		assertEquals(testConcept, obsModel.getConcept());
		Set<Obs> obsGroupMembersTopLevel = obsModel.getGroupMembers();
		assertEquals(1, obsGroupMembersTopLevel.size());
		
		Obs obsModelSecondLevel = obsGroupMembersTopLevel.iterator().next();
		assertPatientAndObservationDate(testDate, obsModelSecondLevel);
		assertEquals(testConcept, obsModelSecondLevel.getConcept());
		Set<Obs> obsGroupMembersSecondLevel = obsModelSecondLevel.getGroupMembers();
		assertEquals(1, obsGroupMembersSecondLevel.size());
		
		Obs obsModelThirdLevel = obsGroupMembersSecondLevel.iterator().next();
		assertPatientAndObservationDate(testDate, obsModelSecondLevel);
		assertEquals("LAB_NOTES", obsModelThirdLevel.getConcept().getPreferredName(ENGLISH).getName());
		assertEquals(LAB_TEST_NOTES, obsModelThirdLevel.getValueText());
	}
	
	private Function<String, Concept> getConceptFunction() {
        return (String conceptName) -> {
            Concept concept = new Concept();
            ConceptDatatype conceptDatatype = new ConceptDatatype();
            conceptDatatype.setHl7Abbreviation(ConceptDatatype.TEXT);
            concept.setDatatype(conceptDatatype);
            ConceptName conceptPreferredName = new ConceptName();
            conceptPreferredName.setName(conceptName);
            conceptPreferredName.setLocale(ENGLISH);
            concept.setPreferredName(conceptPreferredName);
            return concept;
        };
    }
	
	private void assertPatientAndObservationDate(Date testDate, Obs obsModel) {
		assertEquals(LabTestTest.PATIENT_ID, obsModel.getPersonId().intValue());
		assertEquals(testDate, obsModel.getObsDatetime());
		assertNull(obsModel.getOrder());
	}
}
