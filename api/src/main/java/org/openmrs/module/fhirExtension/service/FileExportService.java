package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.parser.IParser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
@Component
public class FileExportService {
	
	private static final String NDJSON_EXTENSION = ".ndjson";
	
	public static final String EXTENSION_ZIP = ".zip";
	
	public static final String FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP = "fhir.export.files.directory";
	
	private AdministrationService adminService;
	
	private IParser parser;
	
	@Autowired
	public FileExportService(@Qualifier("adminService") AdministrationService adminService, IParser parser) {
		this.adminService = adminService;
		this.parser = parser;
	}
	
	public void createAndWriteToFile(List<IBaseResource> fhirResources, String directory) {
		if (fhirResources.isEmpty())
			return;
		String resourceType = fhirResources.get(0).getClass().getSimpleName();
		String basePath = getBaseDirectory();
		Path filePath = Paths.get(basePath, directory, resourceType + NDJSON_EXTENSION);
		createFile(filePath);
		writeToFile(fhirResources, filePath);
	}
	
	public void createDirectory(String uuidFolderName) {
		Path uuidDirectoryPath = Paths.get(getBaseDirectory(), uuidFolderName);
		try {
			Files.createDirectories(uuidDirectoryPath);
		}
		catch (IOException e) {
			log.error("Error while creating directory " + uuidDirectoryPath);
			throw new RuntimeException(e);
		}
	}
	
	public void deleteDirectory(String directory) {
		String basePath = getBaseDirectory();
		Path directoryPath = Paths.get(basePath, directory);
		try {
			FileUtils.deleteDirectory(directoryPath.toFile());
		}
		catch (FileAlreadyExistsException e) {
			log.info("File exists " + directory);
		}
		catch (IOException e) {
			log.error("Error while deleting directory " + directoryPath);
			throw new RuntimeException(e);
		}
	}
	
	public void createZipWithExportedNdjsonFiles(String directoryName) {
		String basePath = getBaseDirectory();
		Path exportedFilesPath = Paths.get(basePath, directoryName);
		Path zipFilePath = Paths.get(basePath, directoryName + EXTENSION_ZIP);
		createFile(zipFilePath);
		createZip(exportedFilesPath, zipFilePath);
	}
	
	private void writeToFile(List<IBaseResource> fhirResources, Path filePath) {
		List<String> stringList = fhirResources.stream().map(iBaseResource -> parser.encodeResourceToString(iBaseResource)).collect(Collectors.toList());
		try {
			FileUtils.writeLines(filePath.toFile(), stringList, true);
		} catch (IOException e) {
			log.error("Exception while processing data for " + filePath);
			throw new RuntimeException(e);
		}
	}
	
	private String getBaseDirectory() {
		String propertyValue = adminService.getGlobalProperty(FHIR_EXPORT_FILES_DIRECTORY_GLOBAL_PROP);
		if (StringUtils.isBlank(propertyValue))
			throw new APIException();
		return propertyValue;
	}
	
	private void createFile(Path filePath) {
		try {
			Files.createFile(filePath);
		}
		catch (FileAlreadyExistsException e) {
			log.info("File exists " + filePath);
		}
		catch (IOException e) {
			log.error("Error while creating file " + filePath);
			throw new RuntimeException(e);
		}
	}
	
	private void createZip(Path exportedNdjsonFilesDirectoryPath, Path destinationZipPath) {
		try {
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destinationZipPath.toFile()));
			Files.walkFileTree(exportedNdjsonFilesDirectoryPath, new SimpleFileVisitor<Path>() {
				
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					zos.putNextEntry(new ZipEntry(exportedNdjsonFilesDirectoryPath.relativize(file).toString()));
					Files.copy(file, zos);
					zos.closeEntry();
					return FileVisitResult.CONTINUE;
				}
			});
			zos.close();
		}
		catch (IOException e) {
			log.error("Error while creating zip file " + destinationZipPath);
			throw new RuntimeException(e);
		}
	}
}
