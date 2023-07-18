package org.openmrs.module.fhirExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.openmrs.api.AdministrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModuleConfiguration {
	
	@Autowired
	@Qualifier("adminService")
	private AdministrationService administrationService;
	
	@Bean
	public IParser getFhirJsonParser() {
		return FhirContext.forR4().newJsonParser();
	}
}
