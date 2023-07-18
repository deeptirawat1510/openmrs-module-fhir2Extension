package org.openmrs.module.fhirExtension.export.impl;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.util.LocaleUtility;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, LocaleUtility.class })
@PowerMockIgnore("javax.management.*")
public class ProcedureFormExportTest {
	
	private static final String PROCEDURE_RECORD = "Procedure Record";
	
	@Mock
	@Qualifier("adminService")
	AdministrationService administrationService;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private ObsService obsService;
	
	@InjectMocks
	private ProcedureFormExport procedureFormExport;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	private List<Obs> procedureRecordObs;
	
	@Before
	public void setUp() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(LocaleUtility.class);
		when(Context.getAdministrationService()).thenReturn(administrationService);
		when(LocaleUtility.getLocalesInOrder()).thenReturn(Collections.singleton(Locale.getDefault()));
		when(Context.getLocale()).thenReturn(Locale.getDefault());
		when(administrationService.getGlobalProperty("fhir.export.procedure.template")).thenReturn(
		    "src/test/resources/procedure-template.properties");
	}
	
	@Test
	public void shouldExportProcedureDataInFhirFormat_whenValidDateRangeProvided() {
		when(conceptService.getConceptByUuid("9bb07482-4ff0-0305-1990-000000000014")).thenReturn(getProcedureRootConcept());
		when(conceptTranslator.toFhirResource(any())).thenReturn(getCodeableConcept());
		procedureRecordObs = getProcedureRecordObs();
		when(
		    obsService.getObservations(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Date.class),
		        any(Date.class), anyBoolean())).thenReturn(procedureRecordObs);
		
		List<IBaseResource> procedureResources = procedureFormExport.export("2023-05-01", "2023-05-31");
		assertNotNull(procedureResources);
		assertEquals(1, procedureResources.size());
	}
	
	@Test
	public void shouldNotExportProcedureData_whenProcedureTemplateUnavailable() {
		when(conceptService.getConceptByUuid(anyString())).thenReturn(null);
		List<IBaseResource> procedureResources = procedureFormExport.export("2023-05-01", "2023-05-31");
		
		assertEquals(0, procedureResources.size());
	}
	
	@Test
	public void shouldThrowException_whenInvalidStartDateProvided() {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Unable to parse the date: 2023-AB-CD");
		
		when(conceptService.getConceptByUuid("9bb07482-4ff0-0305-1990-000000000014")).thenReturn(getProcedureRootConcept());
		
		List<IBaseResource> procedureResources = procedureFormExport.export("2023-AB-CD", "2023-05-31");
		
		assertEquals(0, procedureResources.size());
	}
	
	@Test
	public void shouldExportProcedureDataInFhirFormat_whenNoDateRangeProvided() {
		procedureRecordObs = getProcedureRecordObs();
		when(conceptService.getConceptByUuid("9bb07482-4ff0-0305-1990-000000000014")).thenReturn(getProcedureRootConcept());
		when(conceptTranslator.toFhirResource(any())).thenReturn(getCodeableConcept());
		when(
		    obsService.getObservations(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
		        anyBoolean())).thenReturn(procedureRecordObs);
		
		List<IBaseResource> procedureResources = procedureFormExport.export(null, null);
		assertNotNull(procedureResources);
		assertEquals(1, procedureResources.size());
	}
	
	@Test
	public void shouldReadProcedureAttributesFromGlobalProperties_whenProcedureTemplateUnavailable() {
		when(administrationService.getGlobalProperty("fhir.export.procedure.template")).thenReturn(
		    "/unknown/path/procedure-template.properties");
		when(administrationService.getGlobalProperty("conceptMap.procedure.procedureName")).thenReturn(
		    "9bb07482-4ff0-0305-1990-000000000001");
		when(conceptService.getConceptByUuid(anyString())).thenReturn(null);
		List<IBaseResource> procedureResources = procedureFormExport.export("2023-05-01", "2023-05-31");
		
		assertEquals(0, procedureResources.size());
		verify(administrationService, times(11)).getGlobalProperty(anyString());
	}
	
	private List<Obs> getProcedureRecordObs() {
		Encounter encounter = new Encounter();
		encounter.setUuid("encounter-uuid-1");
		Patient patient = new Patient();
		patient.setUuid("patient-uuid-1");
		
		Obs procedureRecordObs = new Obs(1);
		Concept procedureRecordConcept = new Concept(1);
		ConceptName procedureRecordConceptName = new ConceptName(PROCEDURE_RECORD, Locale.getDefault());
		procedureRecordConcept.setFullySpecifiedName(procedureRecordConceptName);
		procedureRecordObs.setPerson(patient);
		procedureRecordObs.setEncounter(encounter);
		
		Obs codedProcedureObs = new Obs(2);
		Concept procedureNameConcept = new Concept(2);
		procedureNameConcept.setUuid("9bb07482-4ff0-0305-1990-000000000001");
		ConceptName procedureNameConceptFQN = new ConceptName("Name of Procedure performed", Locale.getDefault());
		procedureNameConcept.setFullySpecifiedName(procedureNameConceptFQN);
		Concept hairTransplantConcept = new Concept(3);
		ConceptName hairTransplantConceptFQN = new ConceptName("Hair Transplant", Locale.getDefault());
		hairTransplantConcept.setFullySpecifiedName(hairTransplantConceptFQN);
		codedProcedureObs.setValueCoded(hairTransplantConcept);
		codedProcedureObs.setConcept(procedureNameConcept);
		codedProcedureObs.setPerson(patient);
		codedProcedureObs.setEncounter(encounter);
		
		Obs procedureDatetimeObs = new Obs(3);
		Concept procedureDatetimeConcept = new Concept(4);
		procedureDatetimeConcept.setUuid("9bb07482-4ff0-0305-1990-000000000002");
		ConceptName procedureDatetimeConceptFQN = new ConceptName("Procedure date/time", Locale.getDefault());
		procedureDatetimeConcept.setFullySpecifiedName(procedureDatetimeConceptFQN);
		procedureDatetimeObs.setValueDatetime(Date.from(LocalDate.of(2023, 5, 3).atStartOfDay().toInstant(ZoneOffset.UTC)));
		procedureDatetimeObs.setConcept(procedureDatetimeConcept);
		procedureDatetimeObs.setPerson(patient);
		procedureDatetimeObs.setEncounter(encounter);
		
		Obs codedBodySiteObs = new Obs(4);
		Concept codedBodySiteConcept = new Concept(5);
		codedBodySiteConcept.setUuid("9bb07482-4ff0-0305-1990-000000000004");
		ConceptName codedBodySiteConceptFQN = new ConceptName("Procedure site", Locale.getDefault());
		procedureNameConcept.setFullySpecifiedName(codedBodySiteConceptFQN);
		Concept headConcept = new Concept(6);
		ConceptName headConceptFQN = new ConceptName("Head", Locale.getDefault());
		headConcept.setFullySpecifiedName(headConceptFQN);
		codedBodySiteObs.setValueCoded(headConcept);
		codedBodySiteObs.setConcept(codedBodySiteConcept);
		codedBodySiteObs.setPerson(patient);
		codedBodySiteObs.setEncounter(encounter);
		
		Obs procedureEndDatetimeObs = new Obs(5);
		Concept procedureEndDatetimeConcept = new Concept(7);
		procedureEndDatetimeConcept.setUuid("9bb07482-4ff0-0305-1990-000000000003");
		ConceptName procedureEndDatetimeConceptFQN = new ConceptName("Procedure end date/time", Locale.getDefault());
		procedureEndDatetimeConcept.setFullySpecifiedName(procedureEndDatetimeConceptFQN);
		procedureEndDatetimeObs.setValueDatetime(Date.from(LocalDate.of(2023, 5, 3).atStartOfDay().plusHours(1)
		        .toInstant(ZoneOffset.UTC)));
		procedureEndDatetimeObs.setConcept(procedureEndDatetimeConcept);
		procedureEndDatetimeObs.setPerson(patient);
		procedureEndDatetimeObs.setEncounter(encounter);
		
		procedureRecordObs.addGroupMember(codedProcedureObs);
		procedureRecordObs.addGroupMember(procedureDatetimeObs);
		procedureRecordObs.addGroupMember(codedBodySiteObs);
		procedureRecordObs.addGroupMember(procedureEndDatetimeObs);
		return Collections.singletonList(procedureRecordObs);
	}
	
	private Concept getProcedureRootConcept() {
		Concept procedureRecordConcept = new Concept(1);
		ConceptName procedureRecordConceptName = new ConceptName(PROCEDURE_RECORD, Locale.getDefault());
		procedureRecordConcept.setFullySpecifiedName(procedureRecordConceptName);
		return procedureRecordConcept;
	}
	
	private CodeableConcept getCodeableConcept() {
		CodeableConcept codeableConcept = new CodeableConcept();
		codeableConcept.setText("Splint removal");
		return codeableConcept;
	}
}
