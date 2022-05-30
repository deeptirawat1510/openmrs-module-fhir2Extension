package org.openmrs.module.fhirExtension.domain.observation;

import org.apache.commons.collections.CollectionUtils;
import org.openmrs.Concept;
import org.openmrs.Obs;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LabTestResult {
	
	private Concept testConcept;
	
	private List<Result> results;
	
	public Obs toObsModel(BasicObs basicObs) {
		Set<Obs> labResultObs = results
				.stream()
				.filter(Result::isPresent)
				.map(result -> result.toObsModel(basicObs))
				.collect(Collectors.toSet());
		if(CollectionUtils.isNotEmpty(labResultObs)){
			Obs topLevelObs = basicObs.getObsFactory(testConcept).get();
			Obs labObs = basicObs.getObsFactory(testConcept).get();
			labResultObs.forEach(labObs::addGroupMember);
			topLevelObs.addGroupMember(labObs);
			return topLevelObs;
		}
		return null;
	}
	
	public static class LabTestResultBuilder {
		
		private String labReportUrl;
		
		private String labReportName;
		
		private String labReportNotes;
		
		private Concept testConcept;
		
		public LabTestResultBuilder testConcept(Concept testConcept) {
			this.testConcept = testConcept;
			return this;
		}
		
		public LabTestResultBuilder labReportUrl(String labReportUrl) {
			this.labReportUrl = labReportUrl;
			return this;
		}
		
		public LabTestResultBuilder labReportName(String labReportName) {
			this.labReportName = labReportName;
			return this;
		}
		
		public LabTestResultBuilder labReportNotes(String labReportNotes) {
			this.labReportNotes = labReportNotes;
			return this;
		}
		
		public LabTestResult build() {
			LabTestResult labTestResult = new LabTestResult();
			labTestResult.testConcept = testConcept;
			labTestResult.results = Arrays.asList(new LabReport(labReportUrl), new LabReportName(labReportName),
			    new LabNotes(labReportNotes));
			return labTestResult;
		}
	}
	
}
