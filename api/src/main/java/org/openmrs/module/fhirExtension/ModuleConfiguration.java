package org.openmrs.module.fhirExtension;

import org.openmrs.api.AdministrationService;
import org.openmrs.module.fhir2.narrative.OpenmrsThymeleafNarrativeGenerator;
import org.openmrs.module.fhir2.web.util.NarrativeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModuleConfiguration {
	
	private static final String NARRATIVES_PATH = "classpath:narratives.properties";
	
	@Autowired
	@Qualifier("adminService")
	private AdministrationService administrationService;

	@Autowired
	@Qualifier("messageSourceService")
	private MessageSource messageSource;
	
	@Bean
	public OpenmrsThymeleafNarrativeGenerator openMrsNarrativeGenerator() {
		String narrativePropertiesFiles = NarrativeUtils.getValidatedPropertiesFilePath(NARRATIVES_PATH);
		return new OpenmrsThymeleafNarrativeGenerator(messageSource, narrativePropertiesFiles);
	}
}