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
import static org.junit.Assert.assertNull;
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
	public void shouldNotReturnErrorMessage_whenNoDateRangeProvided() {
		String errorMessage = exportTask.validateParams(null, null);
		assertNull(errorMessage);
	}
	
	@Test
	public void shouldNotReturnErrorMessage_whenValidDateRangeProvided() {
		String errorMessage = exportTask.validateParams("2023-05-01", "2023-05-31");
		assertNull(errorMessage);
	}
	
	@Test
	public void shouldReturnErrorMessage_whenValidStartDateGreaterThanEndDate() {
		String errorMessage = exportTask.validateParams("2023-06-01", "2023-05-31");
		assertNotNull(errorMessage);
		assertEquals("End date [2023-05-31] should be on or after start date [2023-06-01]", errorMessage);
	}
	
	@Test
	public void shouldReturnErrorMessage_whenInvalidStartDateProvided() {
		String errorMessage = exportTask.validateParams("2023-05-AB", "2023-05-31");
		assertNotNull(errorMessage);
		assertEquals("Invalid Date Format [yyyy-mm-dd]", errorMessage);
	}
	
	@Test
	public void shouldReturnErrorMessage_whenInvalidEndDateProvided() {
		String errorMessage = exportTask.validateParams("2023-05-01", "2023-05-AB");
		assertNotNull(errorMessage);
		assertEquals("Invalid Date Format [yyyy-mm-dd]", errorMessage);
	}
	
	private FhirTask mockFhirTask() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		return fhirTask;
	}
}
