package org.openmrs.module.fhirExtension.export.impl;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhirExtension.export.Exporter;
import org.openmrs.parameter.OrderSearchCriteria;
import org.openmrs.parameter.OrderSearchCriteriaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Log4j2
public class ProcedureOrderExport implements Exporter {
	
	public static final String PROCEDURE_ORDER = "Procedure Order";
	
	public static final String SURGICAL_PROCEDURE = "surgical procedure";
	
	private final OrderService orderService;
	
	private final ConceptService conceptService;
	
	private final ConceptTranslator conceptTranslator;
	
	@Autowired
	public ProcedureOrderExport(OrderService orderService, ConceptService conceptService, ConceptTranslator conceptTranslator) {
		this.orderService = orderService;
		this.conceptService = conceptService;
		this.conceptTranslator = conceptTranslator;
	}
	
	@Override
    public List<IBaseResource> export(String startDate, String endDate) {
        List<IBaseResource> procedureResources = new ArrayList<>();
        OrderType procedureOrderType = orderService.getOrderTypeByName(PROCEDURE_ORDER);
        if (procedureOrderType == null) {
            log.error("Order Type " + PROCEDURE_ORDER + " is not available");
            return procedureResources;
        }
        OrderSearchCriteria orderSearchCriteria = getOrderSearchCriteria(procedureOrderType, startDate, endDate);
        List<Order> orders = orderService.getOrders(orderSearchCriteria);
        orders.stream().map(this::convertToFhirResource).forEach(procedureResources :: add);
        return procedureResources;
    }
	
	private ServiceRequest convertToFhirResource(Order order) {
		ServiceRequest serviceRequest = new ServiceRequest();
		
		CodeableConcept codeableConcept = conceptTranslator.toFhirResource(order.getConcept());
		
		serviceRequest.setId(order.getUuid());
		serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
		serviceRequest.setCode(codeableConcept);
		serviceRequest.setSubject(getSubjectReference(order.getPatient().getUuid()));
		serviceRequest.setEncounter(getEncounterReference(order.getEncounter().getUuid()));
		
		Concept surgicalProcedureConcept = conceptService.getConceptByName(SURGICAL_PROCEDURE);
		CodeableConcept serviceRequestCategory = conceptTranslator.toFhirResource(surgicalProcedureConcept);
		serviceRequest.setCategory(Collections.singletonList(serviceRequestCategory));
		return serviceRequest;
	}
	
	private OrderSearchCriteria getOrderSearchCriteria(OrderType procedureOrderType, String startDate, String endDate) {
		OrderSearchCriteriaBuilder orderSearchCriteriaBuilder = new OrderSearchCriteriaBuilder();
		orderSearchCriteriaBuilder.setOrderTypes(Collections.singletonList(procedureOrderType));
		orderSearchCriteriaBuilder.setIncludeVoided(false);
		try {
			if (startDate != null) {
				orderSearchCriteriaBuilder.setActivatedOnOrAfterDate(DateUtils.parseDate(startDate, DATE_FORMAT));
			}
			if (endDate != null) {
				orderSearchCriteriaBuilder.setActivatedOnOrBeforeDate(DateUtils.parseDate(endDate, DATE_FORMAT));
			}
		}
		catch (ParseException e) {
			log.error("Exception while parsing the date ", e);
			throw new RuntimeException("Exception while parsing the date");
		}
		
		return orderSearchCriteriaBuilder.build();
	}
}
