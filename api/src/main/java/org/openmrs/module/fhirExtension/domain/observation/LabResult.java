package org.openmrs.module.fhirExtension.domain.observation;

import lombok.Builder;
import lombok.Getter;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

@Builder
@Getter
public class LabResult {
	
	private Concept concept;
	
	private String labReportUrl;
	
	private String labReportFileName;
	
	private String labReportNotes;
	
	private Object labResultValue;
	
	private Obs.Interpretation interpretationOfLabResultValue;
	
	private BiFunction<Concept, Object, Obs> obsFactory;
	
	public boolean isPanel() {
		return !concept.getSetMembers().isEmpty();
	}
	
	public List<Concept> getAllTests() {
		return this.concept.getSetMembers();
	}
	
	public Obs newObs(Concept testConcept) {
		return obsFactory.apply(testConcept, null);
	}
	
	public Optional<Obs> newValueObs(Concept obsConcept, Object value) {
		if (value != null) {
			return Optional.of(obsFactory.apply(obsConcept, value));
		}
		return Optional.empty();
	}
	
	public boolean isLabReportPresent() {
		return labReportFileName != null || labReportUrl != null;
	}
	
	public static LabResultBuilder builder() {
		return new LabResult.LabResultBuilder();
	}
	
	public static class LabResultBuilder {
		
		private String labReportUrl;
		
		private String labReportNotes;
		
		private String labReportFileName;
		
		public LabResultBuilder labReportUrl(Optional<Obs> obs) {
			obs.ifPresent(labReportUrlObs -> this.labReportUrl = labReportUrlObs.getValueText());
			return this;
		}
		
		public LabResultBuilder labReportUrl(String labReportUrl) {
			this.labReportUrl = labReportUrl;
			return this;
		}
		
		public LabResultBuilder labReportNotes(Optional<Obs> obs) {
			obs.ifPresent(labReportNotesObs -> this.labReportNotes = labReportNotesObs.getValueText());
			return this;
		}
		
		public LabResultBuilder labReportNotes(String labReportNotes) {
			this.labReportNotes = labReportNotes;
			return this;
		}
		
		public LabResultBuilder labReportFileName(Optional<Obs> obs) {
			obs.ifPresent(labReportFileNameObs -> this.labReportFileName = labReportFileNameObs.getValueText());
			return this;
		}
		
		public LabResultBuilder labReportFileName(String labReportFileName) {
			this.labReportFileName = labReportFileName;
			return this;
		}
		
		public LabResultBuilder setLabResultValue(Obs obs) {
			if (obs.getInterpretation() != null) {
				Obs.Interpretation interpretation = obs.getInterpretation();
				this.interpretationOfLabResultValue = interpretation;
			}
			String datatype = obs.getConcept().getDatatype().getHl7Abbreviation();
			if (datatype.equals(ConceptDatatype.NUMERIC))
				this.labResultValue = obs.getValueNumeric().toString();
			else if (datatype.equals(ConceptDatatype.CODED)) {
				this.labResultValue = obs.getValueCoded();
			} else if (datatype.equals(ConceptDatatype.BOOLEAN)) {
				this.labResultValue = obs.getValueBoolean();
			} else if (datatype.equals(ConceptDatatype.DATETIME)) {
				this.labResultValue = obs.getValueDatetime();
			} else if (datatype.equals(ConceptDatatype.DATE)) {
				this.labResultValue = obs.getValueDate();
			} else if (datatype.equals(ConceptDatatype.TIME)) {
				this.labResultValue = obs.getValueTime();
			} else
				this.labResultValue = obs.getValueText();
			return this;
		}
	}
}
