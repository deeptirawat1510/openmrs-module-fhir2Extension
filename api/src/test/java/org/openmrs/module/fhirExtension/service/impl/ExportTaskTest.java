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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
	
	@Test
	public void shouldReturnTrue_whenNoDateRangeProvided() {
		boolean validParams = exportTask.validateParams(null, null);
		assertTrue(validParams);
	}
	
	@Test
	public void shouldReturnTrue_whenValidDateRangeProvided() {
		boolean validParams = exportTask.validateParams("2023-05-01", "2023-05-31");
		assertTrue(validParams);
	}
	
	@Test
	public void shouldReturnFalse_whenInvalidStartDateProvided() {
		boolean validParams = exportTask.validateParams("2023-05-AB", "2023-05-31");
		assertFalse(validParams);
	}
	
	@Test
	public void shouldReturnFalse_whenInvalidEndDateProvided() {
		boolean validParams = exportTask.validateParams("2023-05-01", "2023-05-AB");
		assertFalse(validParams);
	}
	
	private FhirTask mockFhirTask() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		return fhirTask;
	}
}
