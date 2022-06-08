package org.openmrs.module.fhirExtension.translators.impl;

import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DiagnosticReportObsTranslatorHelper {
	
	public static final String LAB_REPORT_CONCEPT = "LAB_REPORT";
	
	public static final String LAB_RESULT_CONCEPT = "LAB_RESULT";
	
	public static final String LAB_NOTES_CONCEPT = "LAB_NOTES";
	
	@Autowired
	private ConceptService conceptService;
	
	public Obs createObs(@Nonnull LabResult labResult) {
        if (labResult.isPanel()) {
            Obs panel = labResult.newObs(labResult.getConcept());
            Set<Obs> allTestsInPanel = labResult.getAllTests().stream().map(testInPanel -> createTestObs(labResult, testInPanel)).collect(Collectors.toSet());
            allTestsInPanel.forEach(panel::addGroupMember);
            return panel;
        } else {
            return createTestObs(labResult, labResult.getConcept());
        }
    }
	
	private Obs createTestObs(LabResult labResult, Concept testConcept) {
        Set<Obs> labResultObs = createLabResultObs(labResult);
        if (CollectionUtils.isNotEmpty(labResultObs)) {
            Obs topLevelObs = labResult.newObs(testConcept);
            Obs labObs = labResult.newObs(testConcept);

            labResultObs.forEach(labObs::addGroupMember);
            topLevelObs.addGroupMember(labObs);
            return topLevelObs;
        }
        return null;
    }
	
	private Set<Obs> createLabResultObs(LabResult labResult) {
        Set<Obs> labResultObs = new HashSet<>();
        labResult.labReportUrl().map(newObsResult(labResult, LAB_REPORT_CONCEPT)).ifPresent(labResultObs::add);
        labResult.labReportFileName().map(newObsResult(labResult, LAB_RESULT_CONCEPT)).ifPresent(labResultObs::add);
        labResult.labReportNotes().map(newObsResult(labResult, LAB_NOTES_CONCEPT)).ifPresent(labResultObs::add);
        return labResultObs;
    }
	
	private Function<String, Obs> newObsResult(LabResult labResult, String conceptName) {
        return resultValue -> {
            Obs obs = labResult.newObs(conceptService.getConceptByName(conceptName));
            try {
                obs.setValueAsString(resultValue);
            } catch (ParseException e) {
                throw new APIException(e);
            }
            return obs;
        };
    }
	
}