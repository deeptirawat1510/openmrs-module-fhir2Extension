package org.openmrs.module.fhirExtension.export.impl;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Procedure;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhirExtension.export.Exporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Log4j2
public class ProcedureFormExport implements Exporter {

	private final AdministrationService adminService;
	
	private final ConceptTranslator conceptTranslator;
	
	private final ConceptService conceptService;
	
	private final ObsService obsService;

	private static final String GP_PROCEDURE_TEMPLATE_PROPERTIES_FILE_PATH = "fhir.export.procedure.template";
	private final Map<ProcedureAttribute, String> procedureAttributesMap = new HashMap<>();
	private final Properties procedureRecordAttributesFromProperties = new Properties();
	private final List<String> procedureConfigurationKeys = new ArrayList<>();
	
	@Autowired
	public ProcedureFormExport(@Qualifier("adminService") AdministrationService adminService,
							   ConceptTranslator conceptTranslator, ConceptService conceptService, ObsService obsService) {
		this.adminService = adminService;
		this.conceptTranslator = conceptTranslator;
		this.conceptService = conceptService;
		this.obsService = obsService;

		Arrays.stream(ProcedureAttribute.values()).forEach(procedureAttribute -> procedureConfigurationKeys.add(procedureAttribute.getMapping()));
	}
	
	@Override
	public List<IBaseResource> export(String startDateStr, String endDateStr) {
		List<IBaseResource> procedureResources = new ArrayList<>();

		try {
			Date startDate = getFormattedDate(startDateStr);
			Date endDate = getFormattedDate(endDateStr);
			readProcedureAttributeProperties();
			Concept procedureRecordConcept = conceptService.getConceptByUuid(procedureAttributesMap.get(getProcedureObsRootConcept()));
			if (procedureRecordConcept == null) {
				log.warn("Procedure Record Template is not available");
				return procedureResources;
			}
			List<Obs> procedureRecordObs = obsService.getObservations(null, null, Collections.singletonList(procedureRecordConcept), null, null,
					null, null, null, null, startDate, endDate, false);
			procedureRecordObs.stream()
							  .map(this::convertToFhirResource)
						      .forEach(procedureResources :: add);
		} catch (Exception e) {
			log.error("Exception while exporting procedure to FHIR type ", e);
			throw new RuntimeException(e);
		}
		return procedureResources;
	}
	
	private Procedure convertToFhirResource(Obs procedureObs) {
		Procedure procedure = new Procedure();
		procedure.setId(procedureObs.getUuid());

		CodeableConcept codeableConcept = conceptTranslator.toFhirResource(procedureObs.getConcept());
		procedure.setCode(codeableConcept);
		procedure.setSubject(getSubjectReference(procedureObs.getPerson().getUuid()));
		procedure.setEncounter(getEncounterReference(procedureObs.getEncounter().getUuid()));
		procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);

		AtomicReference<Date> procedureStartDate = new AtomicReference<>();
		AtomicReference<Date> procedureEndDate = new AtomicReference<>();

		procedureObs.getGroupMembers().forEach(member -> {
			Concept memberConcept = member.getConcept();

			if (conceptMatchesForAttribute(memberConcept, procedureAttributesMap.get(ProcedureAttribute.PROCEDURE_NAME))) {
				procedure.setCode(conceptTranslator.toFhirResource(member.getValueCoded()));
			} else if (conceptMatchesForAttribute(memberConcept, procedureAttributesMap.get(ProcedureAttribute.PROCEDURE_NAME_NONCODED))) {
				CodeableConcept concept = new CodeableConcept();
				concept.setText(member.getValueCoded().getDisplayString());
				procedure.setCode(concept);
			}

			if (conceptMatchesForAttribute(memberConcept, procedureAttributesMap.get(ProcedureAttribute.PROCEDURE_START_DATETIME))) {
				procedureStartDate.set(member.getValueDatetime());
				procedure.setPerformed(new DateTimeType(procedureStartDate.get()));
			}

			if (conceptMatchesForAttribute(memberConcept, procedureAttributesMap.get(ProcedureAttribute.PROCEDURE_END_DATETIME))) {
				procedureEndDate.set(member.getValueDatetime());
			}

			if (conceptMatchesForAttribute(memberConcept, procedureAttributesMap.get(ProcedureAttribute.PROCEDURE_BODYSITE))) {
				procedure.setBodySite(Collections.singletonList(conceptTranslator.toFhirResource(member.getValueCoded())));
			} else if (conceptMatchesForAttribute(memberConcept, procedureAttributesMap.get(ProcedureAttribute.PROCEDURE_NONCODED_BODYSITE))) {
				CodeableConcept concept = new CodeableConcept();
				concept.setText(member.getValueText());
				procedure.setBodySite(Collections.singletonList(concept));
			}

			if (conceptMatchesForAttribute(memberConcept, procedureAttributesMap.get(ProcedureAttribute.PROCEDURE_OUTCOME))) {
				procedure.setOutcome(conceptTranslator.toFhirResource(member.getValueCoded()));
			}
			if (conceptMatchesForAttribute(memberConcept, procedureAttributesMap.get(ProcedureAttribute.PROCEDURE_NOTE))) {
				procedure.addNote(new Annotation(new MarkdownType(member.getValueText())));
			}

		});

		Date currentDate = new Date();
		if (procedureStartDate.get() == null) {
			procedure.setStatus(Procedure.ProcedureStatus.NOTDONE);
		}
		if (procedureStartDate.get() != null && currentDate.compareTo(procedureStartDate.get()) >= 0) {
			if (procedureEndDate.get() == null || currentDate.compareTo(procedureEndDate.get()) <= 0)
				procedure.setStatus(Procedure.ProcedureStatus.INPROGRESS);
		} else {
			procedure.setStatus(Procedure.ProcedureStatus.PREPARATION);
		}
		return procedure;
	}

	public enum ProcedureAttribute {
		PROCEDURE_TEMPLATE("conceptMap.procedure.procedureTemplate"),
		PROCEDURE_NAME("conceptMap.procedure.procedureName"),
		PROCEDURE_NAME_NONCODED("conceptMap.procedure.procedureNameNonCoded"),
		PROCEDURE_START_DATETIME("conceptMap.procedure.procedureStartDate"),
		PROCEDURE_END_DATETIME("conceptMap.procedure.procedureEndDate"),
		PROCEDURE_BODYSITE("conceptMap.procedure.procedureBodySite"),
		PROCEDURE_NONCODED_BODYSITE("conceptMap.procedure.procedureNonCodedBodySite"),
		PROCEDURE_OUTCOME("conceptMap.procedure.procedureOutcome"),
		PROCEDURE_NONCODED_OUTCOME("conceptMap.procedure.procedureOutcomeNonCoded"),
		PROCEDURE_NOTE("conceptMap.procedure.procedureNote");

		private final String mapping;

		ProcedureAttribute(String mapping) {
			this.mapping = mapping;
		}

		public String getMapping() {
			return mapping;
		}
	}

	private ProcedureAttribute getProcedureObsRootConcept() {
		return ProcedureAttribute.PROCEDURE_TEMPLATE;
	}

	private void updateProcedureAttributeConceptsMap() {
		Arrays.stream(ProcedureAttribute.values()).forEach(procedureAttribute -> {
			String procedureAttributeUuid = (String) procedureRecordAttributesFromProperties.get(procedureAttribute.getMapping());
			getProcedureAttributesMap().put(procedureAttribute, procedureAttributeUuid);
		});
	}

	private Map<ProcedureAttribute, String> getProcedureAttributesMap() {
		return procedureAttributesMap;
	}

	private boolean conceptMatchesForAttribute(Concept memberConcept, String mappedConceptUuid) {
		return mappedConceptUuid != null && memberConcept.getUuid().equals(mappedConceptUuid);
	}

	private void readProcedureAttributeProperties() {
		String procedureTemplateGlobalPropValue = adminService.getGlobalProperty(GP_PROCEDURE_TEMPLATE_PROPERTIES_FILE_PATH);
		Path configFilePath = null;
		if ( StringUtils.isNotBlank(procedureTemplateGlobalPropValue) ) {
			configFilePath = Paths.get(procedureTemplateGlobalPropValue);
		}
		if (StringUtils.isEmpty(procedureTemplateGlobalPropValue) || !Files.exists(configFilePath)) {
			log.warn(String.format("Procedure Attribute config file does not exist: [%s]. Trying to read from Global Properties", configFilePath));
			readFromGlobalProperties();
			return;
		}
		log.info(String.format("Reading Procedure Attribute config properties from : %s", configFilePath));
		try (InputStream configFile = Files.newInputStream(configFilePath)) {
			procedureRecordAttributesFromProperties.load(configFile);
			updateProcedureAttributeConceptsMap();
		} catch (IOException e) {
			log.error("Error Occurred while trying to read Procedure Attribute config file", e);
		}
	}

	private void readFromGlobalProperties() {
		procedureConfigurationKeys.forEach(key -> {
			String value = adminService.getGlobalProperty(key);
			if (!StringUtils.isEmpty(value)) {
				procedureRecordAttributesFromProperties.put(key, value);
			} else {
				log.warn("Procedure Attribute: No property set for " + key);
			}
		});
		updateProcedureAttributeConceptsMap();
	}
}