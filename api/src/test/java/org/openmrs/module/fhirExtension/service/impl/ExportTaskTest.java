package org.openmrs.module.fhirExtension.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.Person;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.dao.FhirTaskDao;
import org.openmrs.module.fhir2.model.FhirTask;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
@PowerMockIgnore("javax.management.*")
public class ExportTaskTest {
	
	@Mock
	private FhirTaskDao fhirTaskDao;
	
	@InjectMocks
	private ExportTaskImpl exportTask;
	
	@Before
	public void setUp() {
		PowerMockito.mockStatic(Context.class);
		User authenticatedUser = new User();
		authenticatedUser.setPerson(new Person());
		when(Context.getAuthenticatedUser()).thenReturn(authenticatedUser);
	}
	
	@Test
	public void shouldCreateFhirTask_whenRequestedForPatientDataExport() {
		when(fhirTaskDao.createOrUpdate(any(FhirTask.class))).thenReturn(mockFhirTask());
		
		FhirTask initialTaskResponse = exportTask.getInitialTaskResponse();
		
		assertNotNull(initialTaskResponse);
		assertEquals(FhirTask.TaskStatus.ACCEPTED, initialTaskResponse.getStatus());
	}
	
	private FhirTask mockFhirTask() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		return fhirTask;
	}
}
