package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.model.api.Include;
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
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.translators.DiagnosticReportObsTranslator;
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
	
	@Override
	public DiagnosticReport get(@Nonnull String s) {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		diagnosticReport.setId(s + "4321567");
		return diagnosticReport;
	}
	
	@Override
	public List<DiagnosticReport> get(@Nonnull Collection<String> collection) {
		return null;
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
	public void delete(@Nonnull String s) {
		
	}
	
	@Override
	public IBundleProvider searchForDiagnosticReports(ReferenceAndListParam referenceAndListParam,
	        ReferenceAndListParam referenceAndListParam1, DateRangeParam dateRangeParam,
	        TokenAndListParam tokenAndListParam, ReferenceAndListParam referenceAndListParam2,
	        TokenAndListParam tokenAndListParam1, DateRangeParam dateRangeParam1, SortSpec sortSpec, HashSet<Include> hashSet) {
		return null;
	}
	
	private Set<Obs> createObs(Set<Obs> results) {
		return results.stream()
				.map(obs -> obsService.saveObs(obs, SAVE_OBS_MESSAGE))
				.collect(Collectors.toSet());
	}
}