package org.openmrs.module.fhirExtension.export.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;
import org.openmrs.module.fhir2.api.translators.MedicationTranslator;
import org.openmrs.module.fhirExtension.export.Exporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MedicationRequestExport implements Exporter {
	
	private final FhirMedicationRequestService fhirMedicationRequestService;
	
	private final MedicationTranslator medicationTranslator;
	
	private final OrderService orderService;
	
	@Autowired
	public MedicationRequestExport(FhirMedicationRequestService fhirMedicationRequestService,
	    MedicationTranslator medicationTranslator, OrderService orderService) {
		this.fhirMedicationRequestService = fhirMedicationRequestService;
		this.medicationTranslator = medicationTranslator;
		this.orderService = orderService;
	}
	
	@Override
	public List<IBaseResource> export(String startDate, String endDate) {
		DateRangeParam lastUpdated = getLastUpdated(startDate, endDate);
		IBundleProvider iBundleProvider = fhirMedicationRequestService.searchForMedicationRequests(null, null, null, null,
		    null, null, null, null, lastUpdated, null, null);
		List<IBaseResource> medicationRequests = iBundleProvider.getAllResources().stream().map(this::addMedicationInfo).collect(Collectors.toList());
		return medicationRequests;
	}
	
	private MedicationRequest addMedicationInfo(IBaseResource iBaseResource) {
		MedicationRequest medicationRequest = (MedicationRequest) iBaseResource;
		String orderUuid = medicationRequest.getId();
		Drug drug = ((DrugOrder) orderService.getOrderByUuid(orderUuid)).getDrug();
		Medication medicationFhirResource = medicationTranslator.toFhirResource(drug);
		medicationRequest.setMedication(medicationFhirResource.getCode());
		return medicationRequest;
	}
}
