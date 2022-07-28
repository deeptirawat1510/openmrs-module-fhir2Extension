package org.openmrs.module.fhirExtension.service;

import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class OrderUpdateService {
	
	private final OrderService orderService;
	
	@Autowired
	public OrderUpdateService(OrderService orderService) {
		this.orderService = orderService;
	}
	
	public void updateOrder(DiagnosticReport diagnosticReport, FhirDiagnosticReport fhirDiagnosticReport) {
        Order currentOrder = null;
        if (!diagnosticReport.getBasedOn().isEmpty()) {
            String orderUuid = diagnosticReport.getBasedOn().get(0).getIdentifier().getValue();
            currentOrder = orderService.getOrderByUuid(orderUuid);
        } else {
            Patient patient = fhirDiagnosticReport.getSubject();
            Integer conceptId = fhirDiagnosticReport.getCode().getId();
            List<Order> allOrders = orderService.getAllOrdersByPatient(patient);
            Optional<Order> optionalOrder = allOrders.stream()
                    .filter(order -> !Order.FulfillerStatus.COMPLETED.equals(order.getFulfillerStatus()))
                    .filter(order -> !order.getVoided())
                    .filter((order) -> order.getConcept().getId().equals(conceptId))
                    .findFirst();
            if (optionalOrder.isPresent()) {
                currentOrder = optionalOrder.get();
            }
        }
        if (currentOrder != null)
            currentOrder.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
    }
}
