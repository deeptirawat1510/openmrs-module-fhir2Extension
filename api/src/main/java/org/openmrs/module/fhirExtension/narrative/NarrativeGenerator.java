package org.openmrs.module.fhirExtension.narrative;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.module.fhir2.narrative.OpenmrsThymeleafNarrativeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class NarrativeGenerator {
	
	@Autowired
	@Qualifier("fhirR4")
	private FhirContext fhirContext;
	
	@Autowired
	private OpenmrsThymeleafNarrativeGenerator openMrsNarrativeGenerator;
	
	public void generateNarrative(IBaseResource resource) {
		openMrsNarrativeGenerator.populateResourceNarrative(fhirContext, resource);
	}
}
