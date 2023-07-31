package org.openmrs.module.fhirExtension.web;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhirExtension.service.FileDownloadService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class })
@PowerMockIgnore("javax.management.*")
public class FileDownloadControllerTest {
	
	@InjectMocks
	private FileDownloadController fileDownloadController;
	
	@Mock
	private FileDownloadService fileDownloadService;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		when(Context.getRegisteredComponent("fileDownloadService", FileDownloadService.class)).thenReturn(
		    fileDownloadService);
		HttpServletRequest request = PowerMockito.mock(HttpServletRequest.class);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}
	
	@Test
	public void shouldReturn_File_WhenExists() throws Exception {
		byte[] bytes = new byte[10];
		when(fileDownloadService.getFile(any())).thenReturn(bytes);
		ResponseEntity<?> responseEntity = fileDownloadController.getFile("fileName");
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertThat(Objects.requireNonNull(responseEntity.getHeaders().get("Content-Disposition")).get(0),
		    CoreMatchers.containsString("fileName.zip"));
		assertEquals(bytes, responseEntity.getBody());
	}
	
	@Test
	public void shouldReturn_NotFound_WhenMissingFile() throws Exception {
		when(fileDownloadService.getFile(any())).thenThrow(new IOException());
		ResponseEntity<?> responseEntity = fileDownloadController.getFile("fileName");
		assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
	}
	
}
