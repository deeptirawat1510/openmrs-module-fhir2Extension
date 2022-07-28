package org.openmrs.module.fhirExtension.validators;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.openmrs.module.fhir2.api.util.FhirUtils.createExceptionErrorOperationOutcome;

@Component
public class DiagnosticReportRequestValidator {
	
	static final String INVALID_ORDER_ERROR_MESSAGE = "Given lab order is not prescribed by the doctor";
	
	@Autowired
	private OrderService orderService;
	
	public void validate(DiagnosticReport diagnosticReport) {
		if (diagnosticReport.getBasedOn().size() > 0 && diagnosticReport.getBasedOn().get(0).getReference() != null)
			validateBasedOn(diagnosticReport);
	}
	
	private void validateBasedOn(DiagnosticReport diagnosticReport) {
		String orderUuid = diagnosticReport.getBasedOn().get(0).getIdentifier().getValue();
		Order order = orderService.getOrderByUuid(orderUuid);
		if (order == null || Order.FulfillerStatus.COMPLETED.equals(order.getFulfillerStatus()) || order.getVoided()
		        || !order.getConcept().getUuid().equals(diagnosticReport.getCode().getCoding().get(0).getCode()))
			throw new UnprocessableEntityException(INVALID_ORDER_ERROR_MESSAGE,
			        createExceptionErrorOperationOutcome(INVALID_ORDER_ERROR_MESSAGE));
	}
}
