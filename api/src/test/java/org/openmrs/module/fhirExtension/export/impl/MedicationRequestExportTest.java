package org.openmrs.module.fhirExtension.export.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MedicationRequestExportTest {
	
	@Mock
	private FhirMedicationRequestService fhirMedicationRequestService;
	
	@InjectMocks
	private MedicationRequestExport medicationRequestExport;
	
	@Test
	public void shouldExportMedicationRequest_whenValidDateRangeProvided() {
		when(
		    fhirMedicationRequestService.searchForMedicationRequests(any(), any(), any(), any(), any(), any(), any(), any(),
		        any(), any(), any())).thenReturn(getMockMedicationRequestBundle());
		
		List<IBaseResource> medicationRequestResources = medicationRequestExport.export("2023-05-01", "2023-05-31");
		
		assertNotNull(medicationRequestResources);
		assertEquals(1, medicationRequestResources.size());
	}
	
	private IBundleProvider getMockMedicationRequestBundle() {
		MedicationRequest medicationRequest = new MedicationRequest();
		medicationRequest.setId(UUID.randomUUID().toString());
		medicationRequest.setSubject(new Reference("Patient/123"));
		medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
		IBundleProvider iBundleProvider = new SimpleBundleProvider(Arrays.asList(medicationRequest));
		;
		return iBundleProvider;
	}
}
