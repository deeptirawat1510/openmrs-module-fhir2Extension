package org.openmrs.module.fhirExtension.service;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.model.FhirTask;

public interface ExportTask {
	
	@Authorized(value = { "Export Patient Data" })
	FhirTask getInitialTaskResponse();
	
	@Authorized(value = { "Export Patient Data" })
	String validateParams(String startDate, String endDate);
}
