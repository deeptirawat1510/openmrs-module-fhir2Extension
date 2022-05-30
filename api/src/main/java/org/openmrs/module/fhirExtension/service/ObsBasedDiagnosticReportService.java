package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir2.api.FhirDiagnosticReportService;
import org.openmrs.module.fhir2.api.dao.FhirDiagnosticReportDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.DiagnosticReportTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.translators.DiagnosticReportObsTranslator;
import org.openmrs.module.fhirExtension.validators.DiagnosticReportObsValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@Primary
@Component
public class ObsBasedDiagnosticReportService implements FhirDiagnosticReportService {
	
	static final String SAVE_OBS_MESSAGE = "Created when saving a Fhir Diagnostic Report";
	
	@Autowired
	private FhirDiagnosticReportDao fhirDiagnosticReportDao;
	
	@Autowired
	private DiagnosticReportObsTranslator diagnosticReportObsTranslator;
	
	@Autowired
	private ObsService obsService;
	
	@Autowired
	private DiagnosticReportObsValidator validator;
	
	@Autowired
	private SearchQuery<FhirDiagnosticReport, DiagnosticReport, FhirDiagnosticReportDao, DiagnosticReportObsTranslator> searchQuery;
	
	@Override
	public DiagnosticReport get(@Nonnull String s) {
		return diagnosticReportObsTranslator.toFhirResource(fhirDiagnosticReportDao.get(s));
	}
	
	@Override
	public DiagnosticReport create(@Nonnull DiagnosticReport diagnosticReport) {
		try {
			FhirDiagnosticReport fhirDiagnosticReport = diagnosticReportObsTranslator.toOpenmrsType(diagnosticReport);
			validator.validate(fhirDiagnosticReport);
			Set<Obs> createdObs = createObs(fhirDiagnosticReport.getResults());
			fhirDiagnosticReport.setResults(createdObs);
			return diagnosticReportObsTranslator
			        .toFhirResource(fhirDiagnosticReportDao.createOrUpdate(fhirDiagnosticReport));
		}
		catch (Exception exception) {
			System.out.println("Exception while saving diagnostic report: " + exception.getMessage());
			throw exception;
		}
	}
	
	@Override
	public DiagnosticReport update(@Nonnull String s, @Nonnull DiagnosticReport diagnosticReport) {
		return null;
	}
	
	@Override
	public DiagnosticReport delete(@Nonnull String s) {
		return null;
	}
	
	@Override
	public IBundleProvider searchForDiagnosticReports(ReferenceAndListParam encounterReference,
	        ReferenceAndListParam patientReference, DateRangeParam issueDate, TokenAndListParam code,
	        ReferenceAndListParam result, TokenAndListParam id, DateRangeParam lastUpdated, SortSpec sort) {
		SearchParameterMap theParams = (new SearchParameterMap())
		        .addParameter("encounter.reference.search.handler", encounterReference)
		        .addParameter("patient.reference.search.handler", patientReference)
		        .addParameter("date.range.search.handler", issueDate).addParameter("coded.search.handler", code)
		        .addParameter("result.search.handler", result).addParameter("common.search.handler", "_id.property", id)
		        .addParameter("common.search.handler", "_lastUpdated.property", lastUpdated).setSortSpec(sort);
		return this.searchQuery.getQueryResults(theParams, fhirDiagnosticReportDao, diagnosticReportObsTranslator);
	}
	
	private Set<Obs> createObs(Set<Obs> results) {
		return results.stream()
				.map(obs -> obsService.saveObs(obs, SAVE_OBS_MESSAGE))
				.collect(toSet());
	}
}