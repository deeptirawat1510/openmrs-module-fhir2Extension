package org.openmrs.module.fhirExtension.service.impl;

import lombok.extern.log4j.Log4j2;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.dao.FhirTaskDao;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.service.ExportTask;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Transactional
public class ExportTaskImpl implements ExportTask {
	
	private FhirTaskDao fhirTaskDao;
	
	public ExportTaskImpl(FhirTaskDao fhirTaskDao) {
		this.fhirTaskDao = fhirTaskDao;
	}
	
	@Override
	public FhirTask getInitialTaskResponse() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		String logMessage = "Patient Data Export by " + Context.getAuthenticatedUser().getUsername();
		fhirTask.setName(logMessage);
		log.info(logMessage);
		fhirTask.setIntent(FhirTask.TaskIntent.ORDER);
		
		fhirTaskDao.createOrUpdate(fhirTask);
		
		return fhirTask;
	}
}
