package org.openmrs.module.fhirExtension.translators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsTranslatorHelper;

import java.util.Date;
import java.util.Set;

import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticReportObsTranslatorHelperTest {
    private static final String REPORT_URL = "/100/uploadReport.pdf";
    private static final String REPORT_NAME = "bloodTest.pdf";
    private static final String LAB_TEST_NOTES = "Report is normal";
    private static final int PATIENT_ID = 12345;

    @Mock
    private ConceptService conceptService;

    @InjectMocks
    private final DiagnosticReportObsTranslatorHelper diagnosticReportObsResultTranslatorHelper = new DiagnosticReportObsTranslatorHelper();


    @Test
    public void shouldTranslateToObsWhenTestReportIsUploadedWithLabNotesForSingleTestWithoutAnyOrder(){
        Concept testConcept = new Concept();
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Date testDate = new Date();
        LabResult labResult = new LabResult();
        labResult.setPatient(patient);
        labResult.setObservationDate(testDate);
        labResult.setConcept(testConcept);
        labResult.setLabReportUrl(REPORT_URL);
        labResult.setLabReportFileName(REPORT_NAME);
        labResult.setLabReportNotes(LAB_TEST_NOTES);
        mockConceptServiceGetConceptByName();

        Obs obsModel = diagnosticReportObsResultTranslatorHelper.createObs(labResult);

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
    public void shouldTranslateToObsWhenOnlyLabNotesAreAvailableForSingleTestWithoutAnyOrder() {
        Concept testConcept = new Concept();
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Date testDate = new Date();
        LabResult labResult = new LabResult();
        labResult.setPatient(patient);
        labResult.setObservationDate(testDate);
        labResult.setConcept(testConcept);
        labResult.setLabReportNotes(LAB_TEST_NOTES);
        mockConceptServiceGetConceptByName();

        Obs obsModel = diagnosticReportObsResultTranslatorHelper.createObs(labResult);

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

    @Test
    public void shouldNotTranslateToObsWhenBothTestReportAndNotesAreNotPresentForSingleTestWithoutAnyOrder() {
        Concept testConcept = new Concept();
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Date testDate = new Date();
        LabResult labResult = new LabResult();
        labResult.setPatient(patient);
        labResult.setObservationDate(testDate);
        labResult.setConcept(testConcept);

        Obs obsModel = diagnosticReportObsResultTranslatorHelper.createObs(labResult);

        assertNull(obsModel);
    }
    @Test
    private void mockConceptServiceGetConceptByName() {
        when(conceptService.getConceptByName(anyString())).thenAnswer(parameter -> {
            String conceptName = (String) parameter.getArguments()[0];
            Concept concept = new Concept();
            ConceptDatatype conceptDatatype = new ConceptDatatype();
            conceptDatatype.setHl7Abbreviation(ConceptDatatype.TEXT);
            concept.setDatatype(conceptDatatype);
            ConceptName conceptPreferredName = new ConceptName();
            conceptPreferredName.setName(conceptName);
            conceptPreferredName.setLocale(ENGLISH);
            concept.setPreferredName(conceptPreferredName);
            return concept;
        });
    }

    private void assertPatientAndObservationDate(Date testDate, Obs obsModel) {
        assertEquals(PATIENT_ID, obsModel.getPersonId().intValue());
        assertEquals(testDate, obsModel.getObsDatetime());
        assertNull(obsModel.getOrder());
    }
}