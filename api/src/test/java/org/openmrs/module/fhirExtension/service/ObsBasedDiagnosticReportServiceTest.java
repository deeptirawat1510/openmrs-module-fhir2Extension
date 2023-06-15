package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirDiagnosticReportDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.ObsBasedDiagnosticReportTranslator;
import org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsLabResultTranslatorImpl;
import org.openmrs.module.fhirExtension.validators.DiagnosticReportObsValidator;
import org.openmrs.module.fhirExtension.validators.DiagnosticReportRequestValidator;

import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fhirExtension.service.ObsBasedDiagnosticReportService.SAVE_OBS_MESSAGE;
import static org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsLabResultTranslatorImpl.*;

@RunWith(MockitoJUnitRunner.class)
public class ObsBasedDiagnosticReportServiceTest {
	
	private static final String REPORT_URL = "/100/uploadReport.pdf";
	
	private static final String REPORT_NAME = "bloodTest.pdf";
	
	private static final String LAB_TEST_NOTES = "Report is normal";
	
	@Mock
	private ObsBasedDiagnosticReportTranslator translator;
	
	@Mock
	private OrderService orderService;
	
	@InjectMocks
	private DiagnosticReportRequestValidator diagnosticReportRequestValidator = Mockito
	        .spy(new DiagnosticReportRequestValidator());
	
	@InjectMocks
	private DiagnosticReportObsValidator diagnosticReportObsValidator = Mockito.spy(new DiagnosticReportObsValidator());
	
	@Mock
	private ObservationTranslator observationTranslator;
	
	@Mock
	private DiagnosticReportObsLabResultTranslatorImpl diagnosticReportObsLabResultTranslator = Mockito
	        .spy(new DiagnosticReportObsLabResultTranslatorImpl());
	
	@Mock
	private ObsService obsService;
	
	@Mock
	private FhirDiagnosticReportDao dao;
	
	@Mock
	private SearchQuery<FhirDiagnosticReport, DiagnosticReport, FhirDiagnosticReportDao, ObsBasedDiagnosticReportTranslator, SearchQueryInclude<DiagnosticReport>> mockSearchQuery;
	
	@Mock
	private SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	
	@Mock
	private EncounterService encounterService;
	
	@Mock
	private VisitService visitService;
	
	@Mock
	private ProviderService providerService;
	
	@Mock
	private AdministrationService adminService;
	
	@InjectMocks
	private final ObsBasedDiagnosticReportService obsBasedDiagnosticReportService = new ObsBasedDiagnosticReportService();
	
	@Captor
	ArgumentCaptor<LabResult> labResultArgumentCaptor = ArgumentCaptor.forClass(LabResult.class);
	
	@Test
	public void shouldCreateDiagnosticReportWithObs_and_UpdateOrderStatusToCOMPLETED() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		
		Obs topLevelObs = new Obs();
		Obs labObs = new Obs();
		labObs.setGroupMembers(of(childObs(LAB_REPORT_CONCEPT, REPORT_URL), childObs(LAB_RESULT_CONCEPT, REPORT_NAME),
		    childObs(LAB_NOTES_CONCEPT, LAB_TEST_NOTES)).collect(toSet()));
		topLevelObs.addGroupMember(labObs);
		
		Set<Obs> obsGroupMembersSecondLevel = topLevelObs.getGroupMembers();
		
		Obs obsModelThirdLevel = obsGroupMembersSecondLevel.iterator().next();
		Set<Obs> obsGroupMembersThirdLevel = obsModelThirdLevel.getGroupMembers();
		Obs reportObs = fetchObs(obsGroupMembersThirdLevel, "LAB_REPORT");
		Obs resultObs = fetchObs(obsGroupMembersThirdLevel, "LAB_RESULT");
		Obs notesObs = fetchObs(obsGroupMembersThirdLevel, "LAB_NOTES");
		
		fhirDiagnosticReport.setResults(Collections.singleton(topLevelObs));
		
		Patient patient = new Patient(123);
		Concept concept = new Concept(12);
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setCode(new Concept(12));
		Order order1 = new Order();
		order1.setConcept(concept);
		order1.setUuid("uuid1");
		Order order2 = new Order();
		order2.setUuid("should_not_be_picked");
		order2.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
		CareSetting careSetting = new CareSetting();
		OrderType orderType = new OrderType(1);
		String careSettingName = CareSetting.CareSettingType.OUTPATIENT.toString();
		
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		UserContext mockUserContext = mock(UserContext.class);
		when(mockUserContext.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(mockUserContext.getLocation()).thenReturn(new Location());
		Context.setUserContext(mockUserContext);
		
		DiagnosticReport mockDiagnosticReport = new DiagnosticReport();
		FhirDiagnosticReport updatedFhirDiagnosticReport = new FhirDiagnosticReport();
		when(orderService.getCareSettingByName(careSettingName)).thenReturn(careSetting);
		when(orderService.getOrderTypeByName("Lab Order")).thenReturn(orderType);
		when(orderService.getOrders(patient, careSetting, orderType, false)).thenReturn(Arrays.asList(order1, order2));
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(dao.createOrUpdate(fhirDiagnosticReport)).thenReturn(updatedFhirDiagnosticReport);
		when(translator.toFhirResource(updatedFhirDiagnosticReport)).thenReturn(mockDiagnosticReport);
		
		when(encounterService.getEncounterType("LAB_RESULT")).thenReturn(new EncounterType());
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.singletonList(new Visit()));
		DiagnosticReport actualDiagnosticReport = obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
		
		verify(obsService, times(1)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
		
		assertEquals(mockDiagnosticReport, actualDiagnosticReport);
		verify(orderService, times(1)).getOrders(patient, careSetting, orderType, false);
		
		assertNotNull(reportObs.getOrder());
		assertNotNull(resultObs.getOrder());
		assertNotNull(notesObs.getOrder());
		
		assertEquals(order1.getFulfillerStatus(), Order.FulfillerStatus.COMPLETED);
	}
	
	@Test
	public void shouldCreateDiagnosticReportWithObsBasedonOpenOrder_and_UpdateOrderStatusToCOMPLETED() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		String orderUuid = "uuid-12";
		List<Reference> basedOn = mockBasedOn(orderUuid);
		diagnosticReportToCreate.setBasedOn(basedOn);
		CodeableConcept conceptFromTheRequest = new CodeableConcept();
		conceptFromTheRequest.setCoding(Collections.singletonList(new Coding("HL7", orderUuid, "Test1")));
		diagnosticReportToCreate.setCode(conceptFromTheRequest);
		
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setResults(of(new Obs(), new Obs()).collect(toSet()));
		
		Patient patient = new Patient(123);
		Concept concept = new Concept(12);
		concept.setUuid(orderUuid);
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setCode(new Concept(12));
		Order order1 = new Order();
		order1.setConcept(concept);
		order1.setUuid("uuid1");
		Order order2 = new Order();
		order2.setUuid("should_not_be_picked");
		order2.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
		
		DiagnosticReport mockDiagnosticReport = new DiagnosticReport();
		FhirDiagnosticReport updatedFhirDiagnosticReport = new FhirDiagnosticReport();
		
		when(orderService.getOrderByUuid(any())).thenReturn(order1);
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(dao.createOrUpdate(fhirDiagnosticReport)).thenReturn(updatedFhirDiagnosticReport);
		when(translator.toFhirResource(updatedFhirDiagnosticReport)).thenReturn(mockDiagnosticReport);
		
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		UserContext mockUserContext = mock(UserContext.class);
		when(mockUserContext.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(mockUserContext.getLocation()).thenReturn(new Location());
		Context.setUserContext(mockUserContext);
		when(encounterService.getEncounterType("LAB_RESULT")).thenReturn(new EncounterType());
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.singletonList(new Visit()));
		DiagnosticReport actualDiagnosticReport = obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
		
		verify(obsService, times(2)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
		
		assertEquals(mockDiagnosticReport, actualDiagnosticReport);
		verify(orderService, times(2)).getOrderByUuid(orderUuid);
		
		assertEquals(order1.getFulfillerStatus(), Order.FulfillerStatus.COMPLETED);
	}
	
	@Test
	public void shouldCreateVisitIfNotAlreadyPresent() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		String orderUuid = "uuid-12";
		List<Reference> basedOn = mockBasedOn(orderUuid);
		diagnosticReportToCreate.setBasedOn(basedOn);
		CodeableConcept conceptFromTheRequest = new CodeableConcept();
		conceptFromTheRequest.setCoding(Collections.singletonList(new Coding("HL7", orderUuid, "Test1")));
		diagnosticReportToCreate.setCode(conceptFromTheRequest);
		
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setResults(of(new Obs(), new Obs()).collect(toSet()));
		
		Patient patient = new Patient(123);
		Concept concept = new Concept(12);
		concept.setUuid(orderUuid);
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setCode(new Concept(12));
		Order order1 = new Order();
		order1.setConcept(concept);
		order1.setUuid("uuid1");
		Order order2 = new Order();
		order2.setUuid("should_not_be_picked");
		order2.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
		
		DiagnosticReport mockDiagnosticReport = new DiagnosticReport();
		FhirDiagnosticReport updatedFhirDiagnosticReport = new FhirDiagnosticReport();
		
		when(orderService.getOrderByUuid(any())).thenReturn(order1);
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(dao.createOrUpdate(fhirDiagnosticReport)).thenReturn(updatedFhirDiagnosticReport);
		when(translator.toFhirResource(updatedFhirDiagnosticReport)).thenReturn(mockDiagnosticReport);
		
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		UserContext mockUserContext = mock(UserContext.class);
		when(mockUserContext.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(mockUserContext.getLocation()).thenReturn(new Location());
		Context.setUserContext(mockUserContext);
		when(encounterService.getEncounterType("LAB_RESULT")).thenReturn(new EncounterType());
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.emptyList());
		when(visitService.getVisitTypes(any())).thenReturn(Collections.singletonList(new VisitType()));
		when(visitService.saveVisit(any())).thenReturn(new Visit());
		
		DiagnosticReport actualDiagnosticReport = obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
		
		verify(obsService, times(2)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
		
		assertEquals(mockDiagnosticReport, actualDiagnosticReport);
		verify(orderService, times(2)).getOrderByUuid(orderUuid);
		
		assertEquals(order1.getFulfillerStatus(), Order.FulfillerStatus.COMPLETED);
		verify(visitService, times(1)).saveVisit(any());
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void shouldThrowUnprocessableEntityException_givenDiagnosticReportRequestWithInvalidOrderDetails() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setResults(new HashSet<>(asList(new Obs(), new Obs())));

		List<Reference> basedOn = mockBasedOn();
		diagnosticReportToCreate.setBasedOn(basedOn);

		when(orderService.getOrderByUuid(any())).thenReturn(null);

		obsBasedDiagnosticReportService.create(diagnosticReportToCreate);

		verify(obsService, times(0)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
		verify(dao, times(0)).createOrUpdate(any(FhirDiagnosticReport.class));
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void shouldThrowUnprocessableEntityException_givenDiagnosticReportRequestWithMissingDetails() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		CodeableConcept conceptFromTheRequest = new CodeableConcept();
		conceptFromTheRequest.setCoding(Collections.singletonList(new Coding("HL7", "uuid-12", "Test1")));
		diagnosticReportToCreate.setCode(conceptFromTheRequest);
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setCode(new Concept(12));
		//		 Setting only Results out of Status, Code, Subject and results
		fhirDiagnosticReport.setResults(Collections.singleton(new Obs()));
		
		Order nonMatchingOrder = new Order();
		nonMatchingOrder.setUuid("uuid1");
		Concept nonMatchingConcept = new Concept(12);
		nonMatchingConcept.setUuid("uuid-123");
		nonMatchingOrder.setConcept(nonMatchingConcept);
		
		Patient patient = new Patient(123);
		Concept concept = new Concept(12);
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setCode(new Concept(12));
		Order order1 = new Order();
		order1.setConcept(concept);
		order1.setUuid("uuid1");
		Order order2 = new Order();
		order2.setUuid("should_not_be_picked");
		order2.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
		CareSetting careSetting = new CareSetting();
		OrderType orderType = new OrderType(1);
		String careSettingName = CareSetting.CareSettingType.OUTPATIENT.toString();
		
		doNothing().when(diagnosticReportRequestValidator).validate(diagnosticReportToCreate);
		when(orderService.getCareSettingByName(careSettingName)).thenReturn(careSetting);
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		when(encounterService.getEncounterType("LAB_RESULT")).thenReturn(new EncounterType());
		when(orderService.getOrderTypeByName("Lab Order")).thenReturn(orderType);
		when(orderService.getOrders(patient, careSetting, orderType, false)).thenReturn(Arrays.asList(order1, order2));
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.singletonList(new Visit()));
		
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		UserContext mockUserContext = mock(UserContext.class);
		when(mockUserContext.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(mockUserContext.getLocation()).thenReturn(new Location());
		Context.setUserContext(mockUserContext);
		
		obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
		
		verify(obsService, times(0)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
		verify(dao, times(0)).createOrUpdate(any(FhirDiagnosticReport.class));
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void shouldThrowUnprocessableEntityException_givenDiagnosticReportRequestWithCompletedOrder() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setResults(Collections.singleton(new Obs()));
		List<Reference> basedOn = mockBasedOn();
		diagnosticReportToCreate.setBasedOn(basedOn);
		
		Order completedPendingOrder = new Order();
		completedPendingOrder.setUuid("uuid1");
		completedPendingOrder.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
		
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(orderService.getOrderByUuid(any())).thenReturn(completedPendingOrder);
		
		obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
		
		verify(obsService, times(0)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
		verify(dao, times(0)).createOrUpdate(any(FhirDiagnosticReport.class));
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void shouldThrowUnprocessableEntityException_givenDiagnosticReportRequestWithVoidedOrder() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setResults(new HashSet<>(asList(new Obs(), new Obs())));
		List<Reference> basedOn = mockBasedOn();
		diagnosticReportToCreate.setBasedOn(basedOn);

		Order voidedOrder = new Order();
		voidedOrder.setUuid("uuid1");
		voidedOrder.setVoided(true);

		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(orderService.getOrderByUuid(any())).thenReturn(voidedOrder);

		obsBasedDiagnosticReportService.create(diagnosticReportToCreate);

		verify(obsService, times(0)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
		verify(dao, times(0)).createOrUpdate(any(FhirDiagnosticReport.class));
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void shouldThrowUnprocessableEntityException_givenDiagnosticReportRequestWithNonMatchingOrder() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setResults(new HashSet<>(asList(new Obs(), new Obs())));
		List<Reference> basedOn = mockBasedOn();
		diagnosticReportToCreate.setBasedOn(basedOn);

		CodeableConcept conceptFromTheRequest = new CodeableConcept();
		conceptFromTheRequest.setCoding(Collections.singletonList(new Coding("HL7", "uuid-12", "Test1")));
		diagnosticReportToCreate.setCode(conceptFromTheRequest);

		Order nonMatchingOrder = new Order();
		nonMatchingOrder.setUuid("uuid1");
		Concept nonMatchingConcept = new Concept(12);
		nonMatchingConcept.setUuid("uuid-123");
		nonMatchingOrder.setConcept(nonMatchingConcept);

		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(orderService.getOrderByUuid(any())).thenReturn(nonMatchingOrder);

		obsBasedDiagnosticReportService.create(diagnosticReportToCreate);

		verify(obsService, times(0)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
		verify(dao, times(0)).createOrUpdate(any(FhirDiagnosticReport.class));
	}
	
	@Test
	public void shouldCallQueryResultsWithProperParameters_whenSearchNeedsToBePerformedForPatient() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam();
		
		obsBasedDiagnosticReportService.searchForDiagnosticReports(null, patientReference, null, null, null, null, null,
		    null, null);
		
		ArgumentCaptor<SearchParameterMap> searchParameterArgumentCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
		ArgumentCaptor<FhirDiagnosticReportDao> daoArgumentCaptor = ArgumentCaptor.forClass(FhirDiagnosticReportDao.class);
		ArgumentCaptor<ObsBasedDiagnosticReportTranslator> translatorArgumentCaptor = ArgumentCaptor
		        .forClass(ObsBasedDiagnosticReportTranslator.class);
		ArgumentCaptor<SearchQueryInclude> searchQueryIncludeArgumentCaptor = ArgumentCaptor
		        .forClass(SearchQueryInclude.class);
		verify(mockSearchQuery).getQueryResults(searchParameterArgumentCaptor.capture(), daoArgumentCaptor.capture(),
		    translatorArgumentCaptor.capture(), searchQueryIncludeArgumentCaptor.capture());
		
		assertNotNull(searchParameterArgumentCaptor.getValue().getParameters(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER));
		assertEquals(dao, daoArgumentCaptor.getValue());
		assertEquals(translator, translatorArgumentCaptor.getValue());
		assertEquals(searchQueryInclude, searchQueryIncludeArgumentCaptor.getValue());
	}
	
	@Test
	public void shouldCreateDiagnosticReportWithObsResultAnd_UpdateOrderStatusToCOMPLETED() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		
		Observation observation = new Observation();
		observation.setId("test");
		observation.setStatus(Observation.ObservationStatus.FINAL);
		Type type = new StringType("10");
		observation.setValue(type);
		Reference reference = new Reference();
		reference.setReference("#test");
		reference.setType("Observation");
		reference.setResource(observation);
		
		diagnosticReportToCreate.setResult(Collections.singletonList(reference));
		
		String orderUuid = "uuid-12";
		List<Reference> basedOn = mockBasedOn(orderUuid);
		diagnosticReportToCreate.setBasedOn(basedOn);
		Date issuedDate = new Date();
		
		CodeableConcept conceptFromTheRequest = new CodeableConcept();
		conceptFromTheRequest.setCoding(Collections.singletonList(new Coding("HL7", orderUuid, "Test1")));
		diagnosticReportToCreate.setCode(conceptFromTheRequest);
		
		Patient patient = new Patient(123);
		Concept concept = new Concept(12);
		concept.setUuid(orderUuid);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setHl7Abbreviation(ConceptDatatype.TEXT);
		concept.setDatatype(conceptDatatype);
		
		Obs topLevelObs = new Obs();
		Obs labObs = new Obs();
		labObs.setGroupMembers(of(childObs("", "10.0")).collect(toSet()));
		topLevelObs.addGroupMember(labObs);
		
		Set<Obs> obsGroupMembersSecondLevel = topLevelObs.getGroupMembers();
		
		Obs obsModelThirdLevel = obsGroupMembersSecondLevel.iterator().next();
		Set<Obs> obsGroupMembersThirdLevel = obsModelThirdLevel.getGroupMembers();
		Obs resultValue = fetchObs(obsGroupMembersThirdLevel, "");
		
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setCode(new Concept(12));
		fhirDiagnosticReport.setIssued(issuedDate);
		
		Order order1 = new Order();
		order1.setConcept(concept);
		order1.setUuid("uuid1");
		CareSetting careSetting = new CareSetting();
		OrderType orderType = new OrderType(1);
		String careSettingName = CareSetting.CareSettingType.OUTPATIENT.toString();
		
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		UserContext mockUserContext = mock(UserContext.class);
		when(mockUserContext.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(mockUserContext.getLocation()).thenReturn(new Location());
		Context.setUserContext(mockUserContext);
		
		Obs obs1 = new Obs();
		obs1.setValueNumeric(10.0);
		obs1.setPerson(patient);
		obs1.setConcept(concept);
		obs1.setOrder(order1);
		
		when(orderService.getCareSettingByName(careSettingName)).thenReturn(careSetting);
		when(orderService.getOrderTypeByName("Lab Order")).thenReturn(orderType);
		when(orderService.getOrderByUuid("uuid-12")).thenReturn(order1);
		when(orderService.getOrders(patient, careSetting, orderType, false)).thenReturn(Arrays.asList(order1));
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(dao.createOrUpdate(fhirDiagnosticReport)).thenReturn(fhirDiagnosticReport);
		when(translator.toFhirResource(fhirDiagnosticReport)).thenReturn(diagnosticReportToCreate);
		when(observationTranslator.toOpenmrsType((Observation) diagnosticReportToCreate.getResult().get(0).getResource()))
		        .thenReturn(obs1);
		when(diagnosticReportObsLabResultTranslator.toOpenmrsType(any(LabResult.class))).thenReturn(topLevelObs);

		when(encounterService.getEncounterType("LAB_RESULT")).thenReturn(new EncounterType());
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.singletonList(new Visit()));
		DiagnosticReport actualDiagnosticReport = obsBasedDiagnosticReportService.create(diagnosticReportToCreate);

		verify(obsService, times(1)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
		verify(diagnosticReportObsLabResultTranslator).toOpenmrsType(labResultArgumentCaptor.capture());

		assertEquals(diagnosticReportToCreate, actualDiagnosticReport);
		assertNotNull(resultValue.getOrder());

		assertEquals(order1.getFulfillerStatus(), Order.FulfillerStatus.COMPLETED);
	}
	@Test
	public void shouldUpdateObsWithPerson_IssuedDate_LabResultAnd_UpdateOrderStatusToCOMPLETED() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();

		Observation observation = new Observation();
		observation.setId("test");
		observation.setStatus(Observation.ObservationStatus.FINAL);
		Type type = new StringType("10");
		observation.setValue(type);
		Reference reference = new Reference();
		reference.setReference("#test");
		reference.setType("Observation");
		reference.setResource(observation);

		diagnosticReportToCreate.setResult(Collections.singletonList(reference));

		String orderUuid = "uuid-12";
		List<Reference> basedOn = mockBasedOn(orderUuid);
		diagnosticReportToCreate.setBasedOn(basedOn);
		Date issuedDate = new Date();

		CodeableConcept conceptFromTheRequest = new CodeableConcept();
		conceptFromTheRequest.setCoding(Collections.singletonList(new Coding("HL7", orderUuid, "Test1")));
		diagnosticReportToCreate.setCode(conceptFromTheRequest);

		Patient patient = new Patient(123);
		Concept concept = new Concept(12);
		concept.setUuid(orderUuid);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setHl7Abbreviation(ConceptDatatype.NUMERIC);
		concept.setDatatype(conceptDatatype);

		Obs topLevelObs = new Obs();
		Obs labObs = new Obs();
		labObs.setGroupMembers(of(childObs("", "10.0")).collect(toSet()));
		topLevelObs.addGroupMember(labObs);

		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setCode(new Concept(12));
		fhirDiagnosticReport.setIssued(issuedDate);

		Order order1 = new Order();
		order1.setConcept(concept);
		order1.setUuid("uuid1");
		CareSetting careSetting = new CareSetting();
		OrderType orderType = new OrderType(1);
		String careSettingName = CareSetting.CareSettingType.OUTPATIENT.toString();

		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		UserContext mockUserContext = mock(UserContext.class);
		when(mockUserContext.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(mockUserContext.getLocation()).thenReturn(new Location());
		Context.setUserContext(mockUserContext);

		Obs obs1 = new Obs();
		obs1.setValueNumeric(10.0);
		obs1.setPerson(patient);
		obs1.setConcept(concept);
		obs1.setOrder(order1);

		when(orderService.getCareSettingByName(careSettingName)).thenReturn(careSetting);
		when(orderService.getOrderTypeByName("Lab Order")).thenReturn(orderType);
		when(orderService.getOrderByUuid("uuid-12")).thenReturn(order1);
		when(orderService.getOrders(patient, careSetting, orderType, false)).thenReturn(Arrays.asList(order1));
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(dao.createOrUpdate(fhirDiagnosticReport)).thenReturn(fhirDiagnosticReport);
		when(translator.toFhirResource(fhirDiagnosticReport)).thenReturn(diagnosticReportToCreate);
		when(observationTranslator.toOpenmrsType((Observation) diagnosticReportToCreate.getResult().get(0).getResource()))
				.thenReturn(obs1);
		when(diagnosticReportObsLabResultTranslator.toOpenmrsType(any(LabResult.class))).thenReturn(topLevelObs);

		when(encounterService.getEncounterType("LAB_RESULT")).thenReturn(new EncounterType());
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.singletonList(new Visit()));

		DiagnosticReport actualDiagnosticReport = obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
		verify(diagnosticReportObsLabResultTranslator).toOpenmrsType(labResultArgumentCaptor.capture());

		LabResult labResult = labResultArgumentCaptor.getValue();
		BiFunction<Concept, Object, Obs> obsFactory = labResult.getObsFactory();
		Obs resultObs = obsFactory.apply(concept, "10.0");

		assertEquals(diagnosticReportToCreate, actualDiagnosticReport);
		
		 assertEquals(labResult.getLabResultValue(), "10.0");
		
		assertEquals(patient, resultObs.getPerson());

		assertEquals(issuedDate, resultObs.getObsDatetime());
		assertEquals(concept, resultObs.getConcept());
	}
	
	@Test
	public void shouldCreateDiagnosticReportWithObs_Codeable_Concept_ResultAnd_UpdateOrderStatusToCOMPLETED() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		
		Observation observation = new Observation();
		observation.setId("test");
		observation.setStatus(Observation.ObservationStatus.FINAL);
		Type type = new CodeableConcept();
		observation.setValue(type);
		Reference reference = new Reference();
		reference.setReference("#test");
		reference.setType("Observation");
		reference.setResource(observation);
		
		diagnosticReportToCreate.setResult(Collections.singletonList(reference));
		
		String orderUuid = "uuid-12";
		List<Reference> basedOn = mockBasedOn(orderUuid);
		diagnosticReportToCreate.setBasedOn(basedOn);
		Date issuedDate = new Date();
		
		CodeableConcept conceptFromTheRequest = new CodeableConcept();
		conceptFromTheRequest.setCoding(Collections.singletonList(new Coding("HL7", orderUuid, "Test1")));
		diagnosticReportToCreate.setCode(conceptFromTheRequest);
		
		Patient patient = new Patient(123);
		Concept concept = new Concept(12);
		Concept testConcept = new Concept();
		concept.setUuid(orderUuid);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setHl7Abbreviation(ConceptDatatype.CODED);
		concept.setDatatype(conceptDatatype);
		
		Obs topLevelObs = new Obs();
		Obs labObs = new Obs();
		labObs.setGroupMembers(of(childObs("", testConcept)).collect(toSet()));
		topLevelObs.addGroupMember(labObs);
		
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setCode(new Concept(12));
		fhirDiagnosticReport.setIssued(issuedDate);
		
		Order order1 = new Order();
		order1.setConcept(concept);
		order1.setUuid("uuid1");
		CareSetting careSetting = new CareSetting();
		OrderType orderType = new OrderType(1);
		String careSettingName = CareSetting.CareSettingType.OUTPATIENT.toString();
		
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		UserContext mockUserContext = mock(UserContext.class);
		when(mockUserContext.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(mockUserContext.getLocation()).thenReturn(new Location());
		Context.setUserContext(mockUserContext);
		
		Obs obs1 = new Obs();
		obs1.setPerson(patient);
		obs1.setConcept(concept);
		obs1.setValueCoded(testConcept);
		obs1.setOrder(order1);
		
		when(orderService.getCareSettingByName(careSettingName)).thenReturn(careSetting);
		when(orderService.getOrderTypeByName("Lab Order")).thenReturn(orderType);
		when(orderService.getOrderByUuid("uuid-12")).thenReturn(order1);
		when(orderService.getOrders(patient, careSetting, orderType, false)).thenReturn(Arrays.asList(order1));
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(dao.createOrUpdate(fhirDiagnosticReport)).thenReturn(fhirDiagnosticReport);
		when(translator.toFhirResource(fhirDiagnosticReport)).thenReturn(diagnosticReportToCreate);
		when(observationTranslator.toOpenmrsType((Observation) diagnosticReportToCreate.getResult().get(0).getResource()))
		        .thenReturn(obs1);
		when(diagnosticReportObsLabResultTranslator.toOpenmrsType(any(LabResult.class))).thenReturn(topLevelObs);
		
		when(encounterService.getEncounterType("LAB_RESULT")).thenReturn(new EncounterType());
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.singletonList(new Visit()));
		
		DiagnosticReport actualDiagnosticReport = obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
		verify(diagnosticReportObsLabResultTranslator).toOpenmrsType(labResultArgumentCaptor.capture());
		
		LabResult labResult = labResultArgumentCaptor.getValue();
		BiFunction<Concept, Object, Obs> obsFactory = labResult.getObsFactory();
		Obs resultObs = obsFactory.apply(concept, testConcept);
		
		assertEquals(diagnosticReportToCreate, actualDiagnosticReport);
		
		assertEquals(labResult.getLabResultValue(), testConcept);
		
		assertEquals(patient, resultObs.getPerson());
		
		assertEquals(issuedDate, resultObs.getObsDatetime());
		assertEquals(concept, resultObs.getConcept());
	}
	
	@Test
	public void shouldCreateDiagnosticReportWithObs_Result_With_Interpretation_And_UpdateOrderStatusToCOMPLETED() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		
		Observation observation = new Observation();
		observation.setId("test");
		observation.setStatus(Observation.ObservationStatus.FINAL);
		Type type = new CodeableConcept();
		observation.setValue(type);
		Reference reference = new Reference();
		reference.setReference("#test");
		reference.setType("Observation");
		reference.setResource(observation);
		
		diagnosticReportToCreate.setResultsInterpreter(Collections.singletonList(reference));
		diagnosticReportToCreate.setResult(Collections.singletonList(reference));
		
		String orderUuid = "uuid-12";
		List<Reference> basedOn = mockBasedOn(orderUuid);
		diagnosticReportToCreate.setBasedOn(basedOn);
		Date issuedDate = new Date();
		
		CodeableConcept conceptFromTheRequest = new CodeableConcept();
		conceptFromTheRequest.setCoding(Collections.singletonList(new Coding("HL7", orderUuid, "Test1")));
		diagnosticReportToCreate.setCode(conceptFromTheRequest);
		
		Patient patient = new Patient(123);
		Concept concept = new Concept(12);
		Concept testConcept = new Concept();
		concept.setUuid(orderUuid);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setHl7Abbreviation(ConceptDatatype.CODED);
		concept.setDatatype(conceptDatatype);
		
		Obs topLevelObs = new Obs();
		Obs labObs = new Obs();
		Obs child = childObs("", testConcept);
		child.setInterpretation(Obs.Interpretation.ABNORMAL);
		labObs.setGroupMembers(of(child).collect(toSet()));
		topLevelObs.addGroupMember(labObs);
		
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setCode(new Concept(12));
		fhirDiagnosticReport.setIssued(issuedDate);
		
		Order order1 = new Order();
		order1.setConcept(concept);
		order1.setUuid("uuid1");
		CareSetting careSetting = new CareSetting();
		OrderType orderType = new OrderType(1);
		String careSettingName = CareSetting.CareSettingType.OUTPATIENT.toString();
		
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		UserContext mockUserContext = mock(UserContext.class);
		when(mockUserContext.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(mockUserContext.getLocation()).thenReturn(new Location());
		Context.setUserContext(mockUserContext);
		
		Obs.Interpretation interpretation = Obs.Interpretation.ABNORMAL;
		Obs obs1 = new Obs();
		obs1.setPerson(patient);
		obs1.setConcept(concept);
		obs1.setValueCoded(testConcept);
		obs1.setOrder(order1);
		obs1.setInterpretation(interpretation);
		
		when(orderService.getCareSettingByName(careSettingName)).thenReturn(careSetting);
		when(orderService.getOrderTypeByName("Lab Order")).thenReturn(orderType);
		when(orderService.getOrderByUuid("uuid-12")).thenReturn(order1);
		when(orderService.getOrders(patient, careSetting, orderType, false)).thenReturn(Arrays.asList(order1));
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(diagnosticReportObsValidator).validate(fhirDiagnosticReport);
		when(dao.createOrUpdate(fhirDiagnosticReport)).thenReturn(fhirDiagnosticReport);
		when(translator.toFhirResource(fhirDiagnosticReport)).thenReturn(diagnosticReportToCreate);
		when(observationTranslator.toOpenmrsType((Observation) diagnosticReportToCreate.getResult().get(0).getResource()))
		        .thenReturn(obs1);
		when(diagnosticReportObsLabResultTranslator.toOpenmrsType(any(LabResult.class))).thenReturn(topLevelObs);
		
		when(encounterService.getEncounterType("LAB_RESULT")).thenReturn(new EncounterType());
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.singletonList(new Visit()));
		
		DiagnosticReport actualDiagnosticReport = obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
		verify(diagnosticReportObsLabResultTranslator).toOpenmrsType(labResultArgumentCaptor.capture());
		
		LabResult labResult = labResultArgumentCaptor.getValue();
		BiFunction<Concept, Object, Obs> obsFactory = labResult.getObsFactory();
		Obs resultObs = obsFactory.apply(concept, testConcept);
		
		assertEquals(diagnosticReportToCreate, actualDiagnosticReport);
		
		assertEquals(labResult.getLabResultValue(), testConcept);
		
		assertEquals(labResult.getInterpretationOfLabResultValue(), interpretation);
		
		assertEquals(patient, resultObs.getPerson());
		
		assertEquals(issuedDate, resultObs.getObsDatetime());
		assertEquals(concept, resultObs.getConcept());
	}
	
	private List<Reference> mockBasedOn() {
		return mockBasedOn("order-uuid");
	}
	
	private List<Reference> mockBasedOn(String orderUuid) {
		Reference reference = new Reference("ServiceRequest");
		reference.setDisplay("Platelet Count");
		reference.setIdentifier(new Identifier().setValue(orderUuid));
		return Collections.singletonList(reference);
	}
	
	private Obs fetchObs(Set<Obs> obsSet, String conceptName) {
		return obsSet.stream().filter(obs -> obs.getConcept().getDisplayString().equals(conceptName)).findAny().get();
	}
	
	private Obs childObs(String concept, Object value) {
		Obs obs = new Obs();
		obs.setConcept(newMockConcept(concept));
		if (value instanceof Concept)
			obs.setValueCoded((Concept) value);
		else
			obs.setValueText((String) value);
		return obs;
	}
	
	private Concept newMockConcept(String conceptName) {
		Concept concept = mock(Concept.class);
		when(concept.getDisplayString()).thenReturn(conceptName);
		return concept;
	}
}
