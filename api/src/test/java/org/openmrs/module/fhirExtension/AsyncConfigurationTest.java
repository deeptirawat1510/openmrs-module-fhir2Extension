package org.openmrs.module.fhirExtension;

import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AsyncConfigurationTest {
	
	@Test
	public void testAsyncExecutor() {
		AsyncConfiguration asyncConfiguration = new AsyncConfiguration();
		Executor taskExecutor = asyncConfiguration.threadPoolTaskExecutor();
		
		assertNotNull(taskExecutor);
		assertTrue(taskExecutor instanceof ThreadPoolTaskExecutor);
	}
}
