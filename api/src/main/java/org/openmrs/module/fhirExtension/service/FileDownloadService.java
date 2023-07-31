package org.openmrs.module.fhirExtension.service;

import org.openmrs.annotation.Authorized;

import java.io.IOException;

public interface FileDownloadService {
	
	@Authorized(value = { "Export Patient Data" })
	public byte[] getFile(String filename) throws IOException;
	
}
