package org.openmrs.module.fhirExtension.domain.observation;

import lombok.Builder;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.fhirExtension.domain.observation.LabTestResult.LabTestResultBuilder;

import java.util.Date;
import java.util.function.Function;

@Builder
public class LabTest {
	
	private final Patient patient;
	
	private final Date observationDate;
	
	private final Concept testConcept;
	
	private final String labReportNotes;
	
	private final String labReportUrl;
	
	private final String labReportName;
	
	private final Function<String, Concept> conceptFunction;
	
	public Obs toObsModel() {
		BasicObs basicObs = new BasicObs(patient, observationDate, conceptFunction);
		LabTestResult labTestResult = new LabTestResultBuilder().testConcept(testConcept).labReportUrl(labReportUrl)
		        .labReportName(labReportName).labReportNotes(labReportNotes).build();
		return labTestResult.toObsModel(basicObs);
	}
}
