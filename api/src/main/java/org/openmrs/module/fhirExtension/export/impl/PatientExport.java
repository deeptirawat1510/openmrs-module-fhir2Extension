package org.openmrs.module.fhirExtension.export.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.openmrs.module.fhir2.api.search.param.PatientSearchParams;
import org.openmrs.module.fhirExtension.export.Exporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatientExport implements Exporter {
	
	private FhirPatientService fhirPatientService;
	
	@Autowired
	public PatientExport(FhirPatientService fhirPatientService) {
		this.fhirPatientService = fhirPatientService;
	}
	
	@Override
	public List<IBaseResource> export(String startDate, String endDate) {
		DateRangeParam lastUpdated = getLastUpdated(startDate, endDate);
		PatientSearchParams patientSearchParams = new PatientSearchParams(null, null, null, null, null, null, null, null,
		        null, null, null, null, null, lastUpdated, null, null);
		IBundleProvider iBundleProvider = fhirPatientService.searchForPatients(patientSearchParams);
		return iBundleProvider.getAllResources();
	}
}
