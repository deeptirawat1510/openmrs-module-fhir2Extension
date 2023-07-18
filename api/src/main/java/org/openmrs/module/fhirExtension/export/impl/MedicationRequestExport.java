package org.openmrs.module.fhirExtension.export.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;
import org.openmrs.module.fhirExtension.export.Exporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MedicationRequestExport implements Exporter {
	
	private FhirMedicationRequestService fhirMedicationRequestService;
	
	@Autowired
	public MedicationRequestExport(FhirMedicationRequestService fhirMedicationRequestService) {
		this.fhirMedicationRequestService = fhirMedicationRequestService;
	}
	
	@Override
	public List<IBaseResource> export(String startDate, String endDate) {
		DateRangeParam lastUpdated = getLastUpdated(startDate, endDate);
		IBundleProvider iBundleProvider = fhirMedicationRequestService.searchForMedicationRequests(null, null, null, null,
		    null, null, null, null, lastUpdated, null, null);
		return iBundleProvider.getAllResources();
	}
	
}
