package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.module.fhir2.api.FhirDiagnosticReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Transactional
@Service
public class ObsBasedDiagnosticReportService implements FhirDiagnosticReportService {
	
	public IBundleProvider searchForDiagnosticReports(ReferenceAndListParam referenceAndListParam,
	        ReferenceAndListParam referenceAndListParam1, DateRangeParam dateRangeParam,
	        TokenAndListParam tokenAndListParam, ReferenceAndListParam referenceAndListParam2,
	        TokenAndListParam tokenAndListParam1, DateRangeParam dateRangeParam1, SortSpec sortSpec, HashSet<Include> hashSet) {
		return null;
	}
	
	public DiagnosticReport get(@Nonnull String s) {
		return new DiagnosticReport();
	}
	
	public List<DiagnosticReport> get(@Nonnull Collection<String> collection) {
		return null;
	}
	
	public DiagnosticReport create(@Nonnull DiagnosticReport diagnosticReport) {
		return new DiagnosticReport();
	}
	
	public DiagnosticReport update(@Nonnull String s, @Nonnull DiagnosticReport diagnosticReport) {
		return null;
	}
	
	public DiagnosticReport delete(@Nonnull String s) {
		return null;
	}
}
