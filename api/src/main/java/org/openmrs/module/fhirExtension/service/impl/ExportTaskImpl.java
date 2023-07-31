package org.openmrs.module.fhirExtension.service.impl;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.dao.FhirTaskDao;
import org.openmrs.module.fhir2.model.FhirTask;
import org.openmrs.module.fhirExtension.service.ExportTask;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.openmrs.module.fhirExtension.export.Exporter.DATE_FORMAT;

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
	
	@Override
	public String validateParams(String startDateStr, String endDateStr) {
		Date startDate = null;
		Date endDate = null;
		String validationErrorMessage = null;
		try {
			if (startDateStr != null)
				startDate = DateUtils.parseDateStrictly(startDateStr, DATE_FORMAT);
			if (endDateStr != null)
				endDate = DateUtils.parseDateStrictly(endDateStr, DATE_FORMAT);
		}
		catch (Exception e) {
			validationErrorMessage = "Invalid Date Format [yyyy-mm-dd]";
		}
		if (startDate != null && endDate != null && startDate.after(endDate)) {
			validationErrorMessage = String.format("End date [%s] should be on or after start date [%s]", endDateStr,
			    startDateStr);
		}
		return validationErrorMessage;
	}
}
