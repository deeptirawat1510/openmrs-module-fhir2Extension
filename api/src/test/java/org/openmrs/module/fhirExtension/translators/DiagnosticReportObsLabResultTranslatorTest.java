package org.openmrs.module.fhirExtension.translators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptSet;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsLabResultTranslatorImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
	public void givenLabTest_WhenTestReportIsUploadedWithNotes_ShouldTranslateToObs() {
		Concept testConcept = new Concept();
		LabResult labResult = LabResult.builder().labReportUrl(REPORT_URL).labReportFileName(REPORT_NAME)
		        .labReportNotes(LAB_TEST_NOTES).concept(testConcept).obsFactory(groupedObsFunction()).build();
		mockConceptServiceGetConceptByName();
		
		Obs obsModel = diagnosticReportObsResultTranslatorHelper.toOpenmrsType(labResult);
		
		assertEquals(testConcept, obsModel.getConcept());
		Set<Obs> obsGroupMembersTopLevel = obsModel.getGroupMembers();
		assertEquals(1, obsGroupMembersTopLevel.size());
		
		Obs obsModelSecondLevel = obsGroupMembersTopLevel.iterator().next();
		assertEquals(testConcept, obsModelSecondLevel.getConcept());
		Set<Obs> obsGroupMembersSecondLevel = obsModelSecondLevel.getGroupMembers();
		assertEquals(3, obsGroupMembersSecondLevel.size());
		
		Obs reportObs = fetchObs(obsGroupMembersSecondLevel, "LAB_REPORT");
		assertEquals(REPORT_URL, reportObs.getValueText());
		Obs resultObs = fetchObs(obsGroupMembersSecondLevel, "LAB_RESULT");
		assertEquals(REPORT_NAME, resultObs.getValueText());
		Obs notesObs = fetchObs(obsGroupMembersSecondLevel, "LAB_NOTES");
		assertEquals(LAB_TEST_NOTES, notesObs.getValueText());
	}
	
	@Test
	public void givenLabTest_WhenTestReportIsNotUploaded_ShouldNotTranslateToObs() {
		Concept testConcept = new Concept();
		LabResult labResult = LabResult.builder().concept(testConcept).obsFactory(groupedObsFunction()).build();
		
		Obs obsModel = diagnosticReportObsResultTranslatorHelper.toOpenmrsType(labResult);
		
		assertNull(obsModel);
	}
	
	@Test
	public void givenLabPanel_WhenTestReportIsUploadedWithLabNotes_ShouldTranslateToObs() {
		Concept testConcept1 = new Concept();
		Concept testConcept2 = new Concept();
		Concept panel = new Concept();
		List<ConceptSet> testsConcepts = new ArrayList<>();
		testsConcepts.add(new ConceptSet(testConcept1, (double) 0));
		testsConcepts.add(new ConceptSet(testConcept2, (double) 0));
		panel.setConceptSets(testsConcepts);
		LabResult labResult = LabResult.builder()
				.labReportUrl(REPORT_URL)
				.labReportFileName(REPORT_NAME)
				.labReportNotes(LAB_TEST_NOTES)
				.concept(panel)
				.obsFactory(groupedObsFunction()).build();
		mockConceptServiceGetConceptByName();

		Obs obsModel = diagnosticReportObsResultTranslatorHelper.toOpenmrsType(labResult);

		assertEquals(panel, obsModel.getConcept());
		Set<Obs> obsGroupMembersTestLevel = obsModel.getGroupMembers();
		assertEquals(2, obsGroupMembersTestLevel.size());

		Map<Concept, Obs> testObsMap = obsGroupMembersTestLevel.stream()
				.collect(Collectors.toMap(Obs::getConcept, Function.identity()));

		testsConcepts.forEach(testConcept -> {
			Obs testObs = testObsMap.get(testConcept.getConcept());
			assertNotNull(testObs);

			Set<Obs> obsGroupMembersSecondLevel = testObs.getGroupMembers();
			assertEquals(1, obsGroupMembersSecondLevel.size());

			Obs obsModelThirdLevel = obsGroupMembersSecondLevel.iterator().next();
			Set<Obs> obsGroupMembersThirdLevel = obsModelThirdLevel.getGroupMembers();
			assertEquals(3, obsGroupMembersThirdLevel.size());

			Obs reportObs = fetchObs(obsGroupMembersThirdLevel, "LAB_REPORT");
			assertEquals(REPORT_URL, reportObs.getValueText());
			Obs resultObs = fetchObs(obsGroupMembersThirdLevel, "LAB_RESULT");
			assertEquals(REPORT_NAME, resultObs.getValueText());
			Obs notesObs = fetchObs(obsGroupMembersThirdLevel, "LAB_NOTES");
			assertEquals(LAB_TEST_NOTES, notesObs.getValueText());
		});
	}
	
	@Test
	public void givenObs_WhenLabReportLabResultAndLabNotesArePresent_ShouldTranslateToLabReportUrlNotesAndTitle() {
		Obs topLevelObs = new Obs();
		Obs labObs = new Obs();
		labObs.setGroupMembers(of(childObs(LAB_REPORT_CONCEPT, REPORT_URL), childObs(LAB_RESULT_CONCEPT, REPORT_NAME),
		    childObs(LAB_NOTES_CONCEPT, LAB_TEST_NOTES)).collect(toSet()));
		topLevelObs.addGroupMember(labObs);
		
		LabResult labResult = diagnosticReportObsResultTranslatorHelper.toFhirResource(topLevelObs);
		
		assertEquals(REPORT_URL, labResult.getLabReportUrl());
		assertEquals(REPORT_NAME, labResult.getLabReportFileName());
		assertEquals(LAB_TEST_NOTES, labResult.getLabReportNotes());
	}
	
	@Test
	public void givenObs_WhenLabReportLabResultAndLabNotesAreNotPresent_ShouldNotTranslateToLabReportUrlNotesAndTitle() {
		Obs topLevelObs = new Obs();
		Obs labObs = new Obs();
		labObs.setGroupMembers(of(childObs("AnotherConcept", "Dummy")).collect(toSet()));
		topLevelObs.addGroupMember(labObs);
		
		LabResult labResult = diagnosticReportObsResultTranslatorHelper.toFhirResource(topLevelObs);
		
		assertNull(labResult.getLabReportUrl());
		assertNull(labResult.getLabReportFileName());
		assertNull(labResult.getLabReportNotes());
	}
	
	@Test
	public void givenObs_WhenAbnormalLabResultIsPresent_ShouldTranslateToLabResult() {
		Concept testConcept = new Concept();
		
		LabResult labResult = LabResult.builder().interpretationOfLabResultValue(Obs.Interpretation.ABNORMAL)
		        .concept(testConcept).obsFactory(groupedObsFunction()).build();
		mockConceptServiceGetConceptByName();
		
		Obs obsModel = diagnosticReportObsResultTranslatorHelper.toOpenmrsType(labResult);
		
		assertEquals(testConcept, obsModel.getConcept());
		Set<Obs> obsGroupMembersTopLevel = obsModel.getGroupMembers();
		assertEquals(1, obsGroupMembersTopLevel.size());
		
		Obs obsModelSecondLevel = obsGroupMembersTopLevel.iterator().next();
		
		assertEquals(testConcept, obsModelSecondLevel.getConcept());
		Set<Obs> obsGroupMembersSecondLevel = obsModelSecondLevel.getGroupMembers();
		obsGroupMembersSecondLevel.iterator().next().setInterpretation(Obs.Interpretation.ABNORMAL);
		assertEquals(1, obsGroupMembersSecondLevel.size());
		
		Obs reportObs = fetchObs(obsGroupMembersSecondLevel, "LAB_ABNORMAL");
		assertEquals(Obs.Interpretation.ABNORMAL, reportObs.getInterpretation());
	}
	
	private BiFunction<Concept, Object, Obs> groupedObsFunction() {
		return (concept, value) -> {
			Obs obs = new Obs();
			obs.setConcept(concept);
			if (value instanceof Boolean) {
				obs.setValueBoolean((Boolean) value);
			} else
				obs.setValueText((String) value);
			return obs;
		};
	}
	
	private Obs fetchObs(Set<Obs> obsSet, String conceptName) {
		return obsSet.stream().filter(obs -> obs.getConcept().getDisplayString().equals(conceptName)).findAny().get();
	}
	
	private Obs childObs(String concept, Object value) {
		Obs obs = new Obs();
		obs.setConcept(newMockConcept(concept));
		if (value instanceof Boolean)
			obs.setValueBoolean((Boolean) value);
		else
			obs.setValueText((String) value);
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
