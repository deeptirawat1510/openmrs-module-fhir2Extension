package org.openmrs.module.fhirExtension.domain.observation;

import org.openmrs.Obs;

public interface Result {
	
	boolean isPresent();
	
	Obs toObsModel(BasicObs basicObs);
}
