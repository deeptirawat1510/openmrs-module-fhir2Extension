package org.openmrs.module.fhirExtension.web;

import org.openmrs.module.fhirExtension.service.FileDownloadService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/fhirExtension")
public class FileDownloadController extends BaseRestController {
	
	private FileDownloadService fileDownloadService;
	
	@Autowired
	public FileDownloadController(FileDownloadService fileDownloadService) {
		this.fileDownloadService = fileDownloadService;
	}
	
	@ResponseBody
    @RequestMapping(value = "/export", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE, method = RequestMethod.GET)
    public ResponseEntity<?> getFile(@RequestParam("file") String fileName) {
        byte[] bytes;
        try {
            bytes = fileDownloadService.getFile(fileName);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", getAttachment(fileName))
                .body(bytes);
    }
	
	private String getAttachment(String fileName) {
		return new StringBuilder().append("attachment; filename=\"").append(fileName).append(".zip").append("\"").toString();
	}
}
