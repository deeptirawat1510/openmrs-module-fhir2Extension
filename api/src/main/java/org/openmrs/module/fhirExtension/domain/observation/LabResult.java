package org.openmrs.module.fhirExtension.domain.observation;

import lombok.Getter;
import lombok.Setter;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Person;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Setter
@Getter
public class LabResult {
	private Person patient;

	private Concept concept;

	private Date observationDate;
	
	private String labReportUrl;
	
	private String labReportFileName;
	
	private String labReportNotes;
	
	public boolean isPanel() {
		return concept.getSetMembers().size() > 0;
	}
	
	public List<Concept> getAllTests() {
		return this.concept.getSetMembers();
	}
	
	public Obs newObs(Concept concept) {
		Obs obs = new Obs();
		obs.setPerson(patient);
		obs.setObsDatetime(observationDate);
		obs.setConcept(concept);
		return obs;
		
	}
	
	public Optional<String> labReportUrl() {
		return Optional.ofNullable(labReportUrl);
	}
	
	public Optional<String> labReportFileName() {
		return Optional.ofNullable(labReportFileName);
	}
	
	public Optional<String> labReportNotes() {
		return Optional.ofNullable(labReportNotes);
	}
	
	public boolean isLabReportPresent() {
		return labReportFileName != null || labReportUrl != null;
	}
}