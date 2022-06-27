package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.extern.log4j.Log4j2;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirDiagnosticReportService;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.dao.FhirDiagnosticReportDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.translators.ObsBasedDiagnosticReportTranslator;
import org.openmrs.module.fhirExtension.validators.DiagnosticReportRequestValidator;
import org.openmrs.module.fhirExtension.validators.DiagnosticReportObsValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Component
@Log4j2
public class ObsBasedDiagnosticReportService extends BaseFhirService<DiagnosticReport, FhirDiagnosticReport> implements FhirDiagnosticReportService {
	
	static final String SAVE_OBS_MESSAGE = "Created when saving a Fhir Diagnostic Report";
	
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
	private SearchQuery<FhirDiagnosticReport, DiagnosticReport, FhirDiagnosticReportDao, ObsBasedDiagnosticReportTranslator, SearchQueryInclude<DiagnosticReport>> searchQuery;
	
	@Autowired
	private SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	@Override
	public DiagnosticReport create(@Nonnull DiagnosticReport diagnosticReport) {
		try {
			FhirDiagnosticReport fhirDiagnosticReport = obsBasedDiagnosticReportTranslator.toOpenmrsType(diagnosticReport);
			diagnosticReportObsValidator.validate(fhirDiagnosticReport);
			if (diagnosticReport.getBasedOn().size() > 0 && diagnosticReport.getBasedOn().get(0).getReference() != null)
				diagnosticReportRequestValidator.validate(fhirDiagnosticReport);
			Set<Obs> createdObs = createObs(fhirDiagnosticReport.getResults());
			fhirDiagnosticReport.setResults(createdObs);
			FhirDiagnosticReport createdFhirDiagnosticReport = fhirDiagnosticReportDao.createOrUpdate(fhirDiagnosticReport);
			return obsBasedDiagnosticReportTranslator.toFhirResource(createdFhirDiagnosticReport);
		}
		catch (Exception exception) {
			log.error("Exception while saving diagnostic report: " + exception.getMessage());
			throw exception;
		}
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
	
	private Set<Obs> createObs(Set<Obs> results) {
		return results.stream()
				.map(obs -> obsService.saveObs(obs, SAVE_OBS_MESSAGE))
				.collect(Collectors.toSet());
	}
}
