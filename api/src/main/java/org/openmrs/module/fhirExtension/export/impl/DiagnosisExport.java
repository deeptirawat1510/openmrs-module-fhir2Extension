package org.openmrs.module.fhirExtension.export.impl;

import lombok.extern.log4j.Log4j2;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.openmrs.Concept;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.ConditionClinicalStatusTranslator;
import org.openmrs.module.fhirExtension.export.Exporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@Log4j2
public class DiagnosisExport implements Exporter {
	
	private static final String VISIT_DIAGNOSES = "Visit Diagnoses";
	
	private static final String CODED_DIAGNOSIS = "Coded Diagnosis";
	
	private static final String BAHMNI_DIAGNOSIS_STATUS = "Bahmni Diagnosis Status";
	
	private final ConceptTranslator conceptTranslator;
	
	private final ObsService obsService;
	
	private final ConceptService conceptService;
	
	private ConditionClinicalStatusTranslator conditionClinicalStatusTranslator;
	
	@Autowired
	public DiagnosisExport(ConceptTranslator conceptTranslator,
	    ConditionClinicalStatusTranslator conditionClinicalStatusTranslator, ConceptService conceptService,
	    ObsService obsService) {
		this.conceptTranslator = conceptTranslator;
		this.conditionClinicalStatusTranslator = conditionClinicalStatusTranslator;
		this.conceptService = conceptService;
		this.obsService = obsService;
	}
	
	@Override
	public List<IBaseResource> export(String startDateStr, String endDateStr) {
		List<IBaseResource> fhirResources = new ArrayList<>();

		try {
			Date startDate = getFormattedDate(startDateStr);
			Date endDate = getFormattedDate(endDateStr);
			Concept visitDiagnosesConcept = conceptService.getConceptByName(VISIT_DIAGNOSES);
			List<Obs> visitDiagnosesObs = obsService.getObservations(null, null, Arrays.asList(visitDiagnosesConcept), null, null,
					null, null, null, null, startDate, endDate, false);

			visitDiagnosesObs.stream().filter(this::isCodedDiagnosis)
					.map(this::convertDiagnosisAsFhirCondition)
					.forEach(fhirResources :: add);
		} catch (Exception e) {
			log.error("Exception while exporting diagnosis to FHIR type ", e);
			throw new RuntimeException(e);
		}
		
		return fhirResources;
	}
	
	private Condition convertDiagnosisAsFhirCondition(Obs visitDiagnosisObsGroup) {
		Condition condition = new Condition();
		Obs codedDiagnosisObs = getObsFor(visitDiagnosisObsGroup, CODED_DIAGNOSIS);
		CodeableConcept clinicalStatus = getClinicalStatus(visitDiagnosisObsGroup);
		CodeableConcept codeableConcept = conceptTranslator.toFhirResource(codedDiagnosisObs.getValueCoded());
		condition.setId(codedDiagnosisObs.getUuid());
		condition.setCategory(getCategory());
		condition.setClinicalStatus(clinicalStatus);
		condition.setOnset(new DateTimeType().setValue(codedDiagnosisObs.getObsDatetime()));
		condition.setCode(codeableConcept);
		condition.setSubject(getSubjectReference(codedDiagnosisObs.getPerson().getUuid()));
		condition.setEncounter(getEncounterReference(codedDiagnosisObs.getEncounter().getUuid()));
		condition.getMeta().setLastUpdated(codedDiagnosisObs.getObsDatetime());
		return condition;
	}
	
	private boolean isActiveDiagnosis(Obs visitDiagnosisObsGroup) {
		Obs codedDiagnosisStatusObs = getObsFor(visitDiagnosisObsGroup, BAHMNI_DIAGNOSIS_STATUS);
		return codedDiagnosisStatusObs == null;
	}
	
	private boolean isCodedDiagnosis(Obs visitDiagnosisObsGroup) {
		Obs codedDiagnosisObs = getObsFor(visitDiagnosisObsGroup, CODED_DIAGNOSIS);
		return codedDiagnosisObs != null;
	}
	
	private Obs getObsFor(Obs visitDiagnosisObsGroup, String conceptName) {
		Optional<Obs> optionalObs = visitDiagnosisObsGroup.getGroupMembers()
				.stream()
				.filter(obs -> obs.getConcept().getName().getName().equals(conceptName))
				.findFirst();
		if (optionalObs.isPresent()) {
			return optionalObs.get();
		}
		return null;
	}
	
	private CodeableConcept getClinicalStatus(Obs visitDiagnosisObsGroup) {
		boolean isActiveDiagnosis = isActiveDiagnosis(visitDiagnosisObsGroup);
		ConditionClinicalStatus conditionClinicalStatus = ConditionClinicalStatus.INACTIVE;
		if (isActiveDiagnosis) {
			conditionClinicalStatus = ConditionClinicalStatus.ACTIVE;
		}
		return conditionClinicalStatusTranslator.toFhirResource(conditionClinicalStatus);
	}
	
	private List<CodeableConcept> getCategory() {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = new Coding("http://terminology.hl7.org/CodeSystem/condition-category", "encounter-diagnosis",
		        "Encounter Diagnosis");
		return Collections.singletonList(codeableConcept.addCoding(coding));
	}
}
