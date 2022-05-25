package org.openmrs.module.fhirExtension.domain.observation;

import lombok.AllArgsConstructor;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;

import java.util.Date;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public class BasicObs {
	
	private final Patient patient;
	
	private final Date observationDate;
	
	private final Function<String, Concept> conceptFunctionByName;
	
	public Supplier<Obs> getObsFactory(Concept concept) {
		return () -> newObs(concept);
	}
	
	public Supplier<Obs> getObsFactory(String conceptName) {
		return () -> newObs(conceptFunctionByName.apply(conceptName));
	}
	
	private Obs newObs(Concept concept) {
		Obs obs = new Obs();
		obs.setPerson(patient);
		obs.setObsDatetime(observationDate);
		obs.setConcept(concept);
		return obs;
	}
}
