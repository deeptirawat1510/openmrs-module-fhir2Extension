package org.openmrs.module.fhirExtension;

import org.openmrs.api.AdministrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModuleConfiguration {
	
	@Autowired
	@Qualifier("adminService")
	private AdministrationService administrationService;
}
