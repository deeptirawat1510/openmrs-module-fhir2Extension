package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.extern.log4j.Log4j2;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.VisitType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.APIException;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirDiagnosticReportService;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.dao.FhirDiagnosticReportDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;
import org.openmrs.module.fhirExtension.translators.ObsBasedDiagnosticReportTranslator;
import org.openmrs.module.fhirExtension.translators.impl.DiagnosticReportObsLabResultTranslatorImpl;
import org.openmrs.module.fhirExtension.validators.DiagnosticReportObsValidator;
import org.openmrs.module.fhirExtension.validators.DiagnosticReportRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Primary
@Component
@Log4j2
@Transactional
public class ObsBasedDiagnosticReportService extends BaseFhirService<DiagnosticReport, FhirDiagnosticReport> implements FhirDiagnosticReportService {
	
	static final String SAVE_OBS_MESSAGE = "Created when saving a Fhir Diagnostic Report";
	
	static final String ORDER_TYPE_NAME = "Lab Order";
	
	static final String LOCATION_TAG_SUPPORTS_VISITS = "Visit Location";
	
	public static final String LAB_RESULT_ENC_TYPE = "LAB_RESULT";
	
	public static final String LAB_RESULTS_ENCOUNTER_ROLE = "Supporting services";
	
	public static final String LAB_ENTRY_VISIT_TYPE = "labEntry.visitType";
	
	public static final String DEFAULT_LAB_VISIT_TYPE = "LAB_VISIT";
	
	@Autowired
	private FhirDiagnosticReportDao fhirDiagnosticReportDao;
	
	@Autowired
	private ObsBasedDiagnosticReportTranslator obsBasedDiagnosticReportTranslator;
	
	@Autowired
	private ObsService obsService;
	
	@Autowired
	private DiagnosticReportObsValidator diagnosticReportObsValidator;
	
	@Autowired
	private DiagnosticReportRequestValidator diagnosticReportRequestValidator;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private SearchQuery<FhirDiagnosticReport, DiagnosticReport, FhirDiagnosticReportDao, ObsBasedDiagnosticReportTranslator, SearchQueryInclude<DiagnosticReport>> searchQuery;
	
	@Autowired
	private SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	
	@Autowired
	private EncounterService encounterService;
	
	@Autowired
	private VisitService visitService;
	
	@Autowired
	private ProviderService providerService;
	
	@Autowired
	@Qualifier("adminService")
	private AdministrationService adminService;
	
	@Autowired
	private ObservationTranslator observationTranslator;
	
	@Autowired
	private DiagnosticReportObsLabResultTranslatorImpl diagnosticReportObsLabResultTranslator;
	
	@Override
	public DiagnosticReport create(@Nonnull DiagnosticReport diagnosticReport) {
		try {
			diagnosticReportRequestValidator.validate(diagnosticReport);

			List<Obs> reportResults = diagnosticReport.getResult().stream().map(reference -> {
				IBaseResource obsResource = reference.getResource();
				if ((obsResource != null) && (obsResource instanceof Observation) ) {
					return observationTranslator.toOpenmrsType((Observation) obsResource);
				} else {
					return null;
				}
			}).filter(Objects::nonNull).collect(Collectors.toList());
			diagnosticReport.setResult(Collections.emptyList());

			FhirDiagnosticReport fhirDiagnosticReport = obsBasedDiagnosticReportTranslator.toOpenmrsType(diagnosticReport);
			Set<Obs> attachmentObs = fhirDiagnosticReport.getResults();

			Order order = getOrder(diagnosticReport, fhirDiagnosticReport);
			Encounter encounter = createNewEncounterForReport(fhirDiagnosticReport, order);

			fhirDiagnosticReport.setEncounter(encounter);
			if(attachmentObs.isEmpty()) {
				Obs obs = reportResults.get(0);
				LabResult labResult = LabResult.builder().
						labResultValue(obs.getValueNumeric().toString())
						.concept(fhirDiagnosticReport.getCode())
						.obsFactory(newObs(fhirDiagnosticReport.getSubject(), fhirDiagnosticReport.getIssued()))
						.build();
				fhirDiagnosticReport.setResults(
						Stream.of(diagnosticReportObsLabResultTranslator.toOpenmrsType(labResult))
								.filter(Objects::nonNull)
								.collect(Collectors.toSet()));
			}

			diagnosticReportObsValidator.validate(fhirDiagnosticReport);

			Set<Obs> reportObs = saveReportObs(fhirDiagnosticReport, order, encounter);
			
			fhirDiagnosticReport.setResults(reportObs);

			FhirDiagnosticReport createdFhirDiagnosticReport = fhirDiagnosticReportDao.createOrUpdate(fhirDiagnosticReport);
			updateFulFillerStatus(order);
			return obsBasedDiagnosticReportTranslator.toFhirResource(createdFhirDiagnosticReport);
		} catch (Exception exception) {
			log.error("Exception while saving diagnostic report: " + exception.getMessage());
			throw exception;
		}
	}
	
	private BiFunction<Concept, String, Obs> newObs(Patient subject, Date issued) {
		return (concept, value) -> {
			Obs obs = new Obs();
			obs.setPerson(subject);
			obs.setObsDatetime(issued);
			obs.setConcept(concept);
			setObsValue(obs, value);
			return obs;
		};
	}
	
	private void setObsValue(Obs obs, String value) {
		if (value != null) {
			try {
				obs.setValueAsString(value);
			}
			catch (ParseException e) {
				throw new APIException(e);
			}
		}
	}
	
	static final String UNABLE_TO_PROCESS_DIAGNOSTIC_REPORT = "Can not process Diagnostic Report. Please check with your administrator.";
	
	private Encounter createNewEncounterForReport(FhirDiagnosticReport fhirDiagnosticReport, Order order) {
		if (fhirDiagnosticReport.getEncounter() != null) {
			log.info("Diagnostic Report was submitted with an existing encounter reference. This will be overwritten by a new encounter");
		}

		EncounterType encounterType = encounterService.getEncounterType(LAB_RESULT_ENC_TYPE);
		if (encounterType == null) {
			log.error("Encounter type LAB_RESULT must be defined to support Diagnostic Report");
			throw new RuntimeException(UNABLE_TO_PROCESS_DIAGNOSTIC_REPORT);
		}

		Location location = Context.getUserContext().getLocation(); //TODO if not present get from clinic
		if (location == null) {
			log.error("Logged in location for user is null. Can not identify encounter session.");
			throw new RuntimeException(UNABLE_TO_PROCESS_DIAGNOSTIC_REPORT);
		}

		Optional<Visit> reportOrderVisit = Optional.ofNullable(order).map(ord -> ord.getEncounter()).map(enc -> enc.getVisit());
		Visit applicableVisit = reportOrderVisit.isPresent() ? reportOrderVisit.get() : getActiveVisit(fhirDiagnosticReport).orElseGet(() -> {
			log.warn("Can not identify an active visit for the patient. Trying to identify a lab visit for today...");
			return findOrCreateLabVisit(fhirDiagnosticReport, location);
		});
		if (applicableVisit == null) {
			log.error("Can not identify or create visit for the patient for lab results upload. Please check with your administrator");
			throw new RuntimeException(UNABLE_TO_PROCESS_DIAGNOSTIC_REPORT);
		}

		return encounterService.saveEncounter(newEncounterInstance(fhirDiagnosticReport.getSubject(), encounterType,
				location, applicableVisit, Context.getAuthenticatedUser()));
	}
	
	private Visit findOrCreateLabVisit(FhirDiagnosticReport report, Location location) {
		String labEntryVisitType = adminService.getGlobalProperty(LAB_ENTRY_VISIT_TYPE);
		if (labEntryVisitType == null || "".equals(labEntryVisitType)) {
			labEntryVisitType = DEFAULT_LAB_VISIT_TYPE;
		}
		List<VisitType> labVisitTypes = visitService.getVisitTypes(labEntryVisitType);
		if (CollectionUtils.isEmpty(labVisitTypes)) {
			return null;
		}
		LocalDate today = LocalDate.now();
		Instant startOfDay = today.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
		Instant endOfDate = LocalTime.MAX.atDate(today).atZone(ZoneId.systemDefault()).toInstant();
		Date startDate = Date.from(startOfDay);
		Date endDate = Date.from(endOfDate);

		List<Visit> labVisitsForToday = visitService.getVisits(
				labVisitTypes,
				Collections.singletonList(report.getSubject()),
				null,
				null,
				startDate, null,
				null, endDate,
				null,
				true,
				false);

		if (!CollectionUtils.isEmpty(labVisitsForToday)) {
			return labVisitsForToday.stream().reduce((first, second) -> second).orElse(null);
		}

		Date visitStartDate = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
		Date visitEndDate = Date.from(LocalDateTime.now().plus(Duration.of(5, ChronoUnit.MINUTES)).atZone(ZoneId.systemDefault()).toInstant());
		Visit newVisit = new Visit();
		newVisit.setPatient(report.getSubject());
		newVisit.setVisitType(labVisitTypes.get(0));
		newVisit.setStartDatetime(visitStartDate);
		newVisit.setStopDatetime(visitEndDate);
		newVisit.setEncounters(new HashSet<Encounter>());
		newVisit.setLocation(visitLocationFor(location));
		return visitService.saveVisit(newVisit);
	}
	
	private Encounter newEncounterInstance(Patient patient, EncounterType encounterType, Location location, Visit visit, User user) {
		Collection<Provider> providersForUser = Optional.ofNullable(providerService.getProvidersByPerson(user.getPerson())).orElse(Collections.emptyList());
		Encounter encounter = new Encounter();
		encounter.setVisit(visit);
		encounter.setPatient(patient);
		encounter.setEncounterType(encounterType);
		encounter.setUuid(UUID.randomUUID().toString());
		encounter.setEncounterDatetime(visit.getStartDatetime());
		encounter.setLocation(location);
		EncounterRole encounterRole = getEncounterRoleForLabResults();
		Set<EncounterProvider> encounterProviders = providersForUser.stream().map(prov -> {
			EncounterProvider encounterProvider = new EncounterProvider();
			encounterProvider.setEncounter(encounter);
			encounterProvider.setProvider(prov);
			encounterProvider.setEncounterRole(encounterRole);
			return encounterProvider;
		}).collect(Collectors.toSet());
		encounter.setEncounterProviders(encounterProviders);
		encounter.setCreator(user);
		return encounter;
	}
	
	EncounterRole getEncounterRoleForLabResults() {
		return Optional.ofNullable(encounterService.getEncounterRoleByName(LAB_RESULTS_ENCOUNTER_ROLE)).orElse(
		    encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID));
	}
	
	private Optional<Visit> getActiveVisit(FhirDiagnosticReport report) {
		List<Visit> activeVisits = visitService.getActiveVisitsByPatient(report.getSubject());
		if (CollectionUtils.isEmpty(activeVisits)) {
			return Optional.empty();
		}
		return Optional.of(activeVisits.get(0));
	}
	
	private Set<Obs> saveReportObs(FhirDiagnosticReport fhirDiagnosticReport, Order order, Encounter encounter) {
		String SAVE_OBS_MESSAGE = "Created when saving a Fhir Diagnostic Report";

		Set<Obs> diagnosticObs = fhirDiagnosticReport.getResults();
		updateObsWithOrderAndEncounter(diagnosticObs, order, encounter);
		return diagnosticObs.stream()
				.map(obs -> obsService.saveObs(obs, SAVE_OBS_MESSAGE))
				.collect(Collectors.toSet());
	}
	
	@Override
	protected FhirDao<FhirDiagnosticReport> getDao() {
		return fhirDiagnosticReportDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirDiagnosticReport, DiagnosticReport> getTranslator() {
		return obsBasedDiagnosticReportTranslator;
	}
	
	@Override
	public IBundleProvider searchForDiagnosticReports(ReferenceAndListParam encounterReference,
	        ReferenceAndListParam patientReference, DateRangeParam issueDate, TokenAndListParam code,
	        ReferenceAndListParam result, TokenAndListParam id, DateRangeParam lastUpdated, SortSpec sort,
	        HashSet<Include> includes) {
		SearchParameterMap theParams = new SearchParameterMap()
		        .addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReference)
		        .addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference)
		        .addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, issueDate)
		        .addParameter(FhirConstants.CODED_SEARCH_HANDLER, code)
		        .addParameter(FhirConstants.RESULT_SEARCH_HANDLER, result)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY, id)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.LAST_UPDATED_PROPERTY, lastUpdated)
		        .addParameter(FhirConstants.INCLUDE_SEARCH_HANDLER, includes).setSortSpec(sort);
		return searchQuery.getQueryResults(theParams, fhirDiagnosticReportDao, obsBasedDiagnosticReportTranslator,
		    searchQueryInclude);
	}
	
	private Order getOrder(DiagnosticReport diagnosticReport, FhirDiagnosticReport fhirDiagnosticReport) {

		if (!diagnosticReport.getBasedOn().isEmpty()) {
			String orderUuid = diagnosticReport.getBasedOn().get(0).getIdentifier().getValue();
			return orderService.getOrderByUuid(orderUuid);
		} else {
			String careSettingTypeName = CareSetting.CareSettingType.OUTPATIENT.toString();
			CareSetting careSetting = orderService.getCareSettingByName(careSettingTypeName);
			OrderType orderType = orderService.getOrderTypeByName(ORDER_TYPE_NAME);
			Patient patient = fhirDiagnosticReport.getSubject();
			Integer conceptId = fhirDiagnosticReport.getCode().getId();
			List<Order> allOrders = orderService.getOrders(patient, careSetting, orderType, false);
			Optional<Order> order = allOrders.stream()
					.filter(o -> !Order.FulfillerStatus.COMPLETED.equals(o.getFulfillerStatus()))
					.filter(o -> o.getConcept().getId().equals(conceptId))
					.findFirst();
			return order.orElse(null);
		}
	}
	
	private void updateFulFillerStatus(Order order) {
		if (order != null) {
			order.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
		}
	}
	
	private void updateObsWithOrderAndEncounter(Set<Obs> diagnosticObs, Order order, Encounter encounter) {
		diagnosticObs.forEach(obs -> {
			obs.setOrder(order);
			obs.setEncounter(encounter);
			if (obs.hasGroupMembers()) {
				updateObsWithOrderAndEncounter(obs.getGroupMembers(), order, encounter);
			}
		});
	}
	
	private Location visitLocationFor(Location location) {
		if (location.getParentLocation() == null) {
			return location;
		}
		return location.hasTag(LOCATION_TAG_SUPPORTS_VISITS) ? location : visitLocationFor(location.getParentLocation());
	}
}
