package org.openmrs.module.fhirExtension.export.impl;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.parameter.OrderSearchCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProcedureOrderExportTest {
	
	public static final String PROCEDURE_ORDER = "Procedure Order";
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@InjectMocks
	private ProcedureOrderExport procedureOrderExport;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void shouldExportProcedureDataInFhirFormat_whenValidDateRangeProvided() {
		when(orderService.getOrderTypeByName(PROCEDURE_ORDER)).thenReturn(new OrderType());
		when(conceptTranslator.toFhirResource(any())).thenReturn(getCodeableConcept());
		when(orderService.getOrders(any(OrderSearchCriteria.class))).thenReturn(getMockOpenmrsProcedureOrders());
		
		List<IBaseResource> procedureResources = procedureOrderExport.export("2023-05-01", "2023-05-31");
		assertNotNull(procedureResources);
		assertEquals(1, procedureResources.size());
	}
	
	@Test
	public void shouldNotExportProcedureData_whenProcedureOrderTypeUnavailable() {
		when(orderService.getOrderTypeByName(PROCEDURE_ORDER)).thenReturn(null);
		
		List<IBaseResource> procedureResources = procedureOrderExport.export("2023-05-01", "2023-05-31");
		
		assertEquals(0, procedureResources.size());
	}
	
	@Test
	public void shouldThrowException_whenInvalidStartDateProvided() {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("Exception while parsing the date");
		
		when(orderService.getOrderTypeByName(PROCEDURE_ORDER)).thenReturn(new OrderType());
		
		List<IBaseResource> procedureResources = procedureOrderExport.export("2023-AB-CD", "2023-05-31");
		
		assertEquals(0, procedureResources.size());
	}
	
	@Test
	public void shouldExportProcedureDataInFhirFormat_whenNoDateRangeProvided() {
		when(orderService.getOrderTypeByName(PROCEDURE_ORDER)).thenReturn(new OrderType());
		when(conceptTranslator.toFhirResource(any())).thenReturn(getCodeableConcept());
		when(orderService.getOrders(any(OrderSearchCriteria.class))).thenReturn(getMockOpenmrsProcedureOrders());
		
		List<IBaseResource> procedureResources = procedureOrderExport.export(null, null);
		assertNotNull(procedureResources);
		assertEquals(1, procedureResources.size());
	}
	
	private List<Order> getMockOpenmrsProcedureOrders() {
        List<Order> orders = new ArrayList<>();
        Order order = new Order(1);

        Patient patient = new Patient();
        patient.setUuid("patient-uuid-1");
        Encounter encounter = new Encounter(1);
        encounter.setUuid("encounter-uuid-1");

        order.setUuid(UUID.randomUUID().toString());
        order.setPatient(patient);
        order.setEncounter(encounter);
        order.setFulfillerStatus(Order.FulfillerStatus.RECEIVED);

        orders.add(order);
        return orders;
    }
	
	private CodeableConcept getCodeableConcept() {
		CodeableConcept codeableConcept = new CodeableConcept();
		codeableConcept.setText("Splint removal");
		return codeableConcept;
	}
}
