package org.openmrs.module.fhirExtension.service;

import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class OrderUpdateServiceTest {
	
	@Mock
	private OrderService orderService;
	
	@InjectMocks
	private OrderUpdateService orderUpdateService;
	
	@Test
	public void shouldUpdateOrderFulfillerStatusWhenPendingLabOrderIsUploaded() {
		String orderUuid = "123";
		Identifier identifier = new Identifier();
		identifier.setValue(orderUuid);
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		Reference reference = new Reference("ServiceRequest");
		reference.setDisplay("Platelet Count");
		reference.setIdentifier(identifier);
		List<Reference> basedOn = Collections.singletonList(reference);
		diagnosticReport.setBasedOn(basedOn);
		Order order = new Order();
		order.setUuid(orderUuid);
		
		when(orderService.getOrderByUuid(orderUuid)).thenReturn(order);
		
		orderUpdateService.updateOrder(diagnosticReport, fhirDiagnosticReport);
		
		assertEquals(Order.FulfillerStatus.COMPLETED, order.getFulfillerStatus());
	}
	
	@Test
	public void shouldUpdateOrderFulfillerStatusWhenUploadingReportAgainstAPendingLabOrder() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		Patient patient = new Patient(123);
		Concept concept = new Concept(12);
		fhirDiagnosticReport.setSubject(patient);
		fhirDiagnosticReport.setCode(concept);
		Order order1 = new Order();
		order1.setUuid("uuid1");
		order1.setConcept(concept);
		Order order2 = new Order();
		order2.setUuid("uuid2");
		
		when(orderService.getAllOrdersByPatient(patient)).thenReturn(Arrays.asList(order1, order2));
		
		orderUpdateService.updateOrder(diagnosticReport, fhirDiagnosticReport);
		
		assertEquals(Order.FulfillerStatus.COMPLETED, order1.getFulfillerStatus());
	}
}
