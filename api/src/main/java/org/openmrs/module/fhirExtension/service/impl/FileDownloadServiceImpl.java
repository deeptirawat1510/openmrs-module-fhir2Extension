package org.openmrs.module.fhirExtension.service.impl;

import org.openmrs.api.AdministrationService;
import org.openmrs.module.fhirExtension.service.FileDownloadService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileDownloadServiceImpl implements FileDownloadService {
	
	public static final String FHIR_EXPORT_FILES_DIRECTORY = "fhir.export.files.directory";
	
	private AdministrationService adminService;
	
	public FileDownloadServiceImpl(AdministrationService adminService) {
		this.adminService = adminService;
	}
	
	public byte[] getFile(String filename) throws IOException {
		String fileDirectory = adminService.getGlobalProperty(FHIR_EXPORT_FILES_DIRECTORY);
		Path path = Paths.get(fileDirectory, filename + ".zip");
		return Files.readAllBytes(path);
	}
	
}
