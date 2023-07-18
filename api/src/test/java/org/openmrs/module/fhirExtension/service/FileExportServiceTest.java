package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fhirExtension.service.FileExportService.FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class })
@PowerMockIgnore("javax.management.*")
public class FileExportServiceTest {
	
	@InjectMocks
	private FileExportService fileExportService;
	
	@Mock
	@Qualifier("adminService")
	AdministrationService administrationService;
	
	@Mock
	private UserContext userContext;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Before
	public void setUp() {
		fileExportService = new FileExportService(administrationService, FhirContext.forR4().newJsonParser());
		PowerMockito.mockStatic(Context.class);
		when(Context.getAdministrationService()).thenReturn(administrationService);
	}
	
	@Test
	public void shouldCreateDirectoryForFhirExport_whenValidPathExists() {
		String basePath = System.getProperty("java.io.tmpdir");
		when(administrationService.getGlobalProperty(FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP)).thenReturn(basePath);
		
		String directory = UUID.randomUUID().toString();
		fileExportService.createDirectory(directory);
		
		File directoryPath = new File(basePath, directory);
		assertTrue(directoryPath.exists());
		assertTrue(directoryPath.isDirectory());
	}
	
	@Test(expected = APIException.class)
	public void shouldThrowExceptionForFhirExport_whenBasePathIsUnavailable() {
		String basePath = null;
		when(administrationService.getGlobalProperty(FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP)).thenReturn(basePath);
		
		String directory = UUID.randomUUID().toString();
		fileExportService.createDirectory(directory);
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldThrowExceptionForFhirExport_whenInvalidDirectorySpecified() {
		String basePath = "/abc/def";
		when(administrationService.getGlobalProperty(FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP)).thenReturn(basePath);
		
		String directory = UUID.randomUUID().toString();
		fileExportService.createDirectory(directory);
	}
	
	@Test
	public void shouldCreateAndWriteToNdjsonFile_whenPatientResourcesProvided() throws IOException {
		String basePath = System.getProperty("java.io.tmpdir");
		List<IBaseResource> patientResources = getPatientResources();
		when(administrationService.getGlobalProperty(FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP)).thenReturn(basePath);
		
		String directory = UUID.randomUUID().toString();
		
		fileExportService.createDirectory(directory);
		fileExportService.createAndWriteToFile(patientResources, directory);
		
		File patientNdjsonFile = new File(basePath, directory + "/Patient.ndjson");
		assertTrue(patientNdjsonFile.exists());
		assertFalse(patientNdjsonFile.isDirectory());
		List<String> allLines = Files.readAllLines(Paths.get(patientNdjsonFile.toURI()));
		assertEquals(1, allLines.size());
	}
	
	@Test
	public void shouldNotCreateNdjsonFile_whenPatientResourcesIsEmpty() {
		String basePath = System.getProperty("java.io.tmpdir");
		List<IBaseResource> patientResources = new ArrayList<>();
		when(administrationService.getGlobalProperty(FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP)).thenReturn(basePath);

		String directory = UUID.randomUUID().toString();

		fileExportService.createDirectory(directory);
		fileExportService.createAndWriteToFile(patientResources, directory);

		File patientNdjsonFile = new File(basePath, directory + "/Patient.ndjson");
		assertFalse(patientNdjsonFile.exists());
	}
	
	@Test
	public void shouldCreateZipFile_whenPatientResourcesProvided() throws IOException {
		String basePath = System.getProperty("java.io.tmpdir");
		List<IBaseResource> patientResources = getPatientResources();
		when(administrationService.getGlobalProperty(FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP)).thenReturn(basePath);
		
		String directory = UUID.randomUUID().toString();
		
		fileExportService.createDirectory(directory);
		fileExportService.createAndWriteToFile(patientResources, directory);
		fileExportService.createZipWithExportedNdjsonFiles(directory);
		
		File fhirExportZipFile = new File(basePath, directory + ".zip");
		assertTrue(fhirExportZipFile.exists());
		assertFalse(fhirExportZipFile.isDirectory());
	}
	
	@Test
	public void shouldDeleteDirectoryForFhirExport_afterCreatingZipFile() {
		String basePath = System.getProperty("java.io.tmpdir");
		List<IBaseResource> patientResources = getPatientResources();
		when(administrationService.getGlobalProperty(FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP)).thenReturn(basePath);
		
		String directory = UUID.randomUUID().toString();
		fileExportService.createDirectory(directory);
		fileExportService.createAndWriteToFile(patientResources, directory);
		fileExportService.createZipWithExportedNdjsonFiles(directory);
		fileExportService.deleteDirectory(directory);
		
		File directoryPath = new File(basePath, directory);
		File fhirExportZipFile = new File(basePath, directory + ".zip");
		assertFalse(directoryPath.exists());
		assertTrue(fhirExportZipFile.exists());
		assertFalse(fhirExportZipFile.isDirectory());
	}
	
	private List<IBaseResource> getPatientResources() {
		List<IBaseResource> patientResources = new ArrayList<>();
		Patient patient1 = new Patient();
		patient1.setId(UUID.randomUUID().toString());
		patient1.setActive(true);
		HumanName humanName = new HumanName();
		humanName.addGiven("John");
		humanName.setFamily("Smith");
		patient1.setName(Collections.singletonList(humanName));

		patientResources.add(patient1);
		return patientResources;
	}
}
