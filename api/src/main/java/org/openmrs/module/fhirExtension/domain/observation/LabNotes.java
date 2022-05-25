package org.openmrs.module.fhirExtension.domain.observation;

import org.openmrs.Obs;
import org.openmrs.api.APIException;

import java.text.ParseException;

public class LabNotes implements Result {
	
	private final String labReportNotes;
	
	public LabNotes(String labReportNotes) {
		this.labReportNotes = labReportNotes;
	}
	
	@Override
	public boolean isPresent() {
		return labReportNotes != null;
	}
	
	@Override
	public Obs toObsModel(BasicObs basicObs) {
		Obs labNotesObs = basicObs.getObsFactory("LAB_NOTES").get();
		try {
			labNotesObs.setValueAsString(labReportNotes);
		}
		catch (ParseException e) {
			throw new APIException(e);
		}
		return labNotesObs;
	}
	
}
