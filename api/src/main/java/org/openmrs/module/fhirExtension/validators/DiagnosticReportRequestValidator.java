package org.openmrs.module.fhirExtension.validators;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.openmrs.module.fhir2.api.util.FhirUtils.createExceptionErrorOperationOutcome;

@Component
public class DiagnosticReportRequestValidator {
	
	static final String INVALID_ORDER_ERROR_MESSAGE = "Given lab order is not prescribed by the doctor";
	
	@Autowired
	private OrderService orderService;
	
	public void validate(FhirDiagnosticReport fhirDiagnosticReport) {
		validateBasedOn(fhirDiagnosticReport);
	}
	
	private void validateBasedOn(FhirDiagnosticReport fhirDiagnosticReport) {
        Patient patient = fhirDiagnosticReport.getSubject();
        Integer conceptId = fhirDiagnosticReport.getCode().getId();
        List<Order> allOrders = orderService.getAllOrdersByPatient(patient);
        long matchingOrdersCount = allOrders.stream().filter(Order::isActive).filter((order) -> order.getConcept().getId().equals(conceptId)).count();
        if(matchingOrdersCount < 1)
            throw new UnprocessableEntityException(INVALID_ORDER_ERROR_MESSAGE, createExceptionErrorOperationOutcome(INVALID_ORDER_ERROR_MESSAGE));
    }
}
