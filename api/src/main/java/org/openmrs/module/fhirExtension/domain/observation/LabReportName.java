package org.openmrs.module.fhirExtension.domain.observation;

import org.openmrs.Obs;
import org.openmrs.api.APIException;

import java.text.ParseException;

public class LabReportName implements Result {
	
	private final String reportName;
	
	public LabReportName(String reportName) {
		this.reportName = reportName;
	}
	
	@Override
	public boolean isPresent() {
		return reportName != null;
	}
	
	@Override
	public Obs toObsModel(BasicObs basicObs) {
		Obs labReportNameObs = basicObs.getObsFactory("LAB_RESULT").get();
		try {
			labReportNameObs.setValueAsString(reportName);
		}
		catch (ParseException e) {
			throw new APIException(e);
		}
		return labReportNameObs;
	}
	
}
