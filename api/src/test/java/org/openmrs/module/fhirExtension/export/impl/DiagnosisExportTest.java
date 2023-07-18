package org.openmrs.module.fhirExtension.export.impl;

import org.hl7.fhir.instance.model.api.IBaseResource;
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
import org.openmrs.module.fhir2.api.translators.ConditionClinicalStatusTranslator;
import org.openmrs.util.LocaleUtility;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, LocaleUtility.class })
@PowerMockIgnore("javax.management.*")
public class DiagnosisExportTest {
	
	private static final String CODED_DIAGNOSIS = "Coded Diagnosis";
	
	private static final String BAHMNI_DIAGNOSIS_STATUS = "Bahmni Diagnosis Status";
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private ObsService obsService;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private ConditionClinicalStatusTranslator conditionClinicalStatusTranslator;
	
	@Mock
	@Qualifier("adminService")
	AdministrationService administrationService;
	
	@InjectMocks
	private DiagnosisExport diagnosisExport;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Before
	public void setUp() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(LocaleUtility.class);
		when(Context.getAdministrationService()).thenReturn(administrationService);
		when(LocaleUtility.getLocalesInOrder()).thenReturn(Collections.singleton(Locale.getDefault()));
		when(Context.getLocale()).thenReturn(Locale.getDefault());
	}
	
	@Test
	public void shouldExportDiagnosis_whenValidDateRangeProvided() {
		List<Obs> visitDiagnosesObs = Stream
		        .concat(getVisitDiagnosesObs().stream(), getInactiveVisitDiagnosesObs().stream()).collect(
		            Collectors.toList());
		when(
		    obsService.getObservations(any(), any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(),
		        anyBoolean())).thenReturn(visitDiagnosesObs);
		
		List<IBaseResource> diagnosisResources = diagnosisExport.export("2023-05-01", "2023-05-31");
		
		assertNotNull(diagnosisResources);
		assertEquals(2, diagnosisResources.size());
		
	}
	
	@Test
	public void shouldExportAllDiagnosis_whenNoDateRangeProvided() {
		List<Obs> visitDiagnosesObs = getVisitDiagnosesObs();
		when(
		    obsService.getObservations(any(), any(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(),
		        anyBoolean())).thenReturn(visitDiagnosesObs);
		
		List<IBaseResource> diagnosisResources = diagnosisExport.export(null, null);
		
		assertNotNull(diagnosisResources);
		assertEquals(1, diagnosisResources.size());
		
	}
	
	@Test
	public void shouldThrowException_whenInvalidStartDateProvided() {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("java.text.ParseException: Unable to parse the date: 2023-AB-CD");
		
		diagnosisExport.export("2023-AB-CD", "2023-05-31");
	}
	
	@Test
	public void shouldThrowException_whenInvalidEndDateProvided() {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("java.text.ParseException: Unable to parse the date: 2023-AB-CD");
		
		diagnosisExport.export("2023-05-01", "2023-AB-CD");
	}
	
	private List<Obs> getVisitDiagnosesObs() {
		Obs visitDiagnosisObs = new Obs(1);
		
		Obs codedDiagnosisObs = new Obs(2);
		Concept malariaConcept = new Concept(1);
		ConceptName malariaConceptFQN = new ConceptName("Malaria", Locale.getDefault());
		malariaConcept.setFullySpecifiedName(malariaConceptFQN);
		codedDiagnosisObs.setValueCoded(malariaConcept);
		
		Concept codedDiagnosisConcept = new Concept(2);
		ConceptName codedDiagnosisConceptName = new ConceptName(CODED_DIAGNOSIS, Locale.getDefault());
		codedDiagnosisConcept.setFullySpecifiedName(codedDiagnosisConceptName);
		codedDiagnosisObs.setConcept(codedDiagnosisConcept);
		Encounter encounter = new Encounter();
		encounter.setUuid("encounter-uuid");
		Patient patient = new Patient();
		patient.setUuid("patient-uuid-1");
		codedDiagnosisObs.setPerson(patient);
		codedDiagnosisObs.setEncounter(encounter);
		visitDiagnosisObs.addGroupMember(codedDiagnosisObs);
		return Collections.singletonList(visitDiagnosisObs);
	}
	
	private List<Obs> getInactiveVisitDiagnosesObs() {
		List<Obs> visitDiagnosesObs = getVisitDiagnosesObs();
		
		Obs codedDiagnosisStatusObs = new Obs(5);
		Concept codedDiagnosisStatusConcept = new Concept(5);
		Concept ruledoutStatusConcept = new Concept(5);
		ConceptName codedDiagnosisStatusConceptName = new ConceptName(BAHMNI_DIAGNOSIS_STATUS, Locale.getDefault());
		codedDiagnosisStatusConcept.setFullySpecifiedName(codedDiagnosisStatusConceptName);
		codedDiagnosisStatusObs.setConcept(codedDiagnosisStatusConcept);
		codedDiagnosisStatusObs.setValueCoded(ruledoutStatusConcept);
		
		visitDiagnosesObs.get(0).addGroupMember(codedDiagnosisStatusObs);
		
		return visitDiagnosesObs;
	}
}
