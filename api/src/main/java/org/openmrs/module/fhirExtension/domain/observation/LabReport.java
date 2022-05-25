package org.openmrs.module.fhirExtension.domain.observation;

import org.openmrs.Obs;
import org.openmrs.api.APIException;

import java.text.ParseException;

public class LabReport implements Result {
	
	private final String labReportUrl;
	
	public LabReport(String labReportUrl) {
		this.labReportUrl = labReportUrl;
	}
	
	@Override
	public boolean isPresent() {
		return labReportUrl != null;
	}
	
	@Override
	public Obs toObsModel(BasicObs basicObs) {
		Obs labReportObs = basicObs.getObsFactory("LAB_REPORT").get();
		try {
			labReportObs.setValueAsString(labReportUrl);
		}
		catch (ParseException e) {
			throw new APIException(e);
		}
		return labReportObs;
	}
	
}
