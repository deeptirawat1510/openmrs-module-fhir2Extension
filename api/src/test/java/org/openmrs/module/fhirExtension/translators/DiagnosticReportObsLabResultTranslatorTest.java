package org.openmrs.module.fhirExtension.translators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsLabResultTranslatorImpl;

import java.util.Set;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsLabResultTranslatorImpl.LAB_NOTES_CONCEPT;
import static org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsLabResultTranslatorImpl.LAB_REPORT_CONCEPT;
import static org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsLabResultTranslatorImpl.LAB_RESULT_CONCEPT;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticReportObsLabResultTranslatorTest {

    private static final String REPORT_URL = "/100/uploadReport.pdf";

    private static final String REPORT_NAME = "bloodTest.pdf";

    private static final String LAB_TEST_NOTES = "Report is normal";

    @Mock
    private ConceptService conceptService;

    @InjectMocks
    private final DiagnosticReportObsLabResultTranslatorImpl diagnosticReportObsResultTranslatorHelper = new DiagnosticReportObsLabResultTranslatorImpl();

    @Test
    public void shouldTranslateToObsWhenTestReportIsUploadedWithLabNotesForSingleTestWithoutAnyOrder() {
        Concept testConcept = new Concept();
        LabResult labResult = LabResult.builder()
                .labReportUrl(REPORT_URL)
                .labReportFileName(REPORT_NAME)
                .labReportNotes(LAB_TEST_NOTES)
                .concept(testConcept)
                .obsFactory(groupedObsFunction()).build();
        mockConceptServiceGetConceptByName();

        Obs obsModel = diagnosticReportObsResultTranslatorHelper.toOpenmrsType(labResult);

        assertEquals(testConcept, obsModel.getConcept());
        Set<Obs> obsGroupMembersTopLevel = obsModel.getGroupMembers();
        assertEquals(1, obsGroupMembersTopLevel.size());

        Obs obsModelSecondLevel = obsGroupMembersTopLevel.iterator().next();
        assertEquals(testConcept, obsModelSecondLevel.getConcept());
        Set<Obs> obsGroupMembersSecondLevel = obsModelSecondLevel.getGroupMembers();
        assertEquals(3, obsGroupMembersSecondLevel.size());

        obsGroupMembersSecondLevel.forEach(obsModelChildLevel -> {
            String obsChildConceptName = obsModelChildLevel.getConcept().getDisplayString();
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
        LabResult labResult = LabResult.builder().labReportNotes(LAB_TEST_NOTES).concept(testConcept)
                .obsFactory(groupedObsFunction()).build();
        mockConceptServiceGetConceptByName();

        Obs obsModel = diagnosticReportObsResultTranslatorHelper.toOpenmrsType(labResult);

        assertEquals(testConcept, obsModel.getConcept());
        Set<Obs> obsGroupMembersTopLevel = obsModel.getGroupMembers();
        assertEquals(1, obsGroupMembersTopLevel.size());

        Obs obsModelSecondLevel = obsGroupMembersTopLevel.iterator().next();
        assertEquals(testConcept, obsModelSecondLevel.getConcept());
        Set<Obs> obsGroupMembersSecondLevel = obsModelSecondLevel.getGroupMembers();
        assertEquals(1, obsGroupMembersSecondLevel.size());

        Obs obsModelThirdLevel = obsGroupMembersSecondLevel.iterator().next();
        assertEquals("LAB_NOTES", obsModelThirdLevel.getConcept().getDisplayString());
        assertEquals(LAB_TEST_NOTES, obsModelThirdLevel.getValueText());
    }

    @Test
    public void shouldNotTranslateToObsWhenBothTestReportAndNotesAreNotPresentForSingleTestWithoutAnyOrder() {
        Concept testConcept = new Concept();
        LabResult labResult = LabResult.builder().concept(testConcept).obsFactory(groupedObsFunction()).build();

        Obs obsModel = diagnosticReportObsResultTranslatorHelper.toOpenmrsType(labResult);

        assertNull(obsModel);
    }

    @Test
    public void shouldTranslateToLabReportUrlNotesAndTitleWhenItIsPresentInObs() {
        Obs topLevelObs = new Obs();
        Obs labObs = new Obs();
        labObs.setGroupMembers(of(childObs(LAB_REPORT_CONCEPT, REPORT_URL),
                childObs(LAB_RESULT_CONCEPT, REPORT_NAME), childObs(LAB_NOTES_CONCEPT, LAB_TEST_NOTES)).collect(
                toSet()));
        topLevelObs.addGroupMember(labObs);

        LabResult labResult = diagnosticReportObsResultTranslatorHelper.toFhirResource(topLevelObs);

        assertEquals(REPORT_URL, labResult.getLabReportUrl());
        assertEquals(REPORT_NAME, labResult.getLabReportFileName());
        assertEquals(LAB_TEST_NOTES, labResult.getLabReportNotes());
    }

    @Test
    public void shouldTranslateToLabReportNotesWhenItIsOnlyPresentInObs() {
        Obs topLevelObs = new Obs();
        Obs labObs = new Obs();
        labObs.setGroupMembers(of(childObs(LAB_NOTES_CONCEPT, LAB_TEST_NOTES)).collect(toSet()));
        topLevelObs.addGroupMember(labObs);

        LabResult labResult = diagnosticReportObsResultTranslatorHelper.toFhirResource(topLevelObs);

        assertEquals(LAB_TEST_NOTES, labResult.getLabReportNotes());
    }

    private BiFunction<Concept, String, Obs> groupedObsFunction() {
        return (concept, value) -> {
            Obs obs = new Obs();
            obs.setConcept(concept);
            obs.setValueText(value);
            return obs;
        };
    }

    private Obs childObs(String concept, String value) {
        Obs obs = new Obs();
        obs.setConcept(newMockConcept(concept));
        obs.setValueText(value);
        return obs;
    }

    private void mockConceptServiceGetConceptByName() {
        when(conceptService.getConceptByName(anyString())).thenAnswer(parameter -> {
            String conceptName = (String) parameter.getArguments()[0];
            return newMockConcept(conceptName);
        });
    }

    private Concept newMockConcept(String conceptName) {
        Concept concept = mock(Concept.class);
        when(concept.getDisplayString()).thenReturn(conceptName);
        return concept;
    }

}