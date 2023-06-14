package org.openmrs.module.fhirExtension.translators.impl;

import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.DiagnosticReportObsLabResultTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class DiagnosticReportObsLabResultTranslatorImpl implements DiagnosticReportObsLabResultTranslator {
	
	public static final String LAB_REPORT_CONCEPT = "LAB_REPORT";
	
	public static final String LAB_RESULT_CONCEPT = "LAB_RESULT";
	
	public static final String LAB_NOTES_CONCEPT = "LAB_NOTES";
	
	@Autowired
	private ConceptService conceptService;
	
	@Override
	public LabResult toFhirResource(@Nonnull Obs obs) {
		return LabResult.builder().labReportUrl(getLabResultChildObs(obs, LAB_REPORT_CONCEPT))
		        .labReportNotes(getLabResultChildObs(obs, LAB_NOTES_CONCEPT))
		        .labReportFileName(getLabResultChildObs(obs, LAB_RESULT_CONCEPT)).build();
	}
	
	@Override
	public Obs toOpenmrsType(@Nonnull LabResult labResult) {
		if (labResult.isPanel()) {
			Obs panel = labResult.newObs(labResult.getConcept());
			labResult.getAllTests().stream()
					.map(testInPanel -> createTestObs(labResult, testInPanel))
					.filter(Objects::nonNull)
					.forEach(panel::addGroupMember);
			return panel;
		} else {
			return createTestObs(labResult, labResult.getConcept());
		}
	}
	
	private Obs createTestObs(LabResult labResult, Concept testConcept) {
        Set<Obs> labResultObs = createLabResultObs(labResult);
        if(CollectionUtils.isNotEmpty(labResultObs)){
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
        labResult.newValueObs(conceptService.getConceptByName(LAB_REPORT_CONCEPT), labResult.getLabReportUrl())
                .ifPresent(labResultObs::add);
        labResult.newValueObs(conceptService.getConceptByName(LAB_RESULT_CONCEPT), labResult.getLabReportFileName())
                .ifPresent(labResultObs::add);
        labResult.newValueObs(conceptService.getConceptByName(LAB_NOTES_CONCEPT), labResult.getLabReportNotes())
                .ifPresent(labResultObs::add);
		labResult.newValueObs(labResult.getConcept(), labResult.getLabResultValue())
					.ifPresent(labResultObs::add);
        return labResultObs;
    }
	
	private Optional<Obs> getLabResultChildObs(Obs obs, String conceptName) {
		for (Obs childObs : obs.getGroupMembers()) {
			if (CollectionUtils.isNotEmpty(childObs.getGroupMembers())) {
				return getLabResultChildObs(childObs, conceptName);
			}
			if (conceptName.equalsIgnoreCase(childObs.getConcept().getDisplayString())) {
				return Optional.of(childObs);
			}
		}
		return Optional.empty();
	}
}
