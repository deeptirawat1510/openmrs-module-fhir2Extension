package org.openmrs.module.fhirExtension.validators;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticReportRequestValidatorTest {
	
	@Mock
	private OrderService orderService;
	
	@InjectMocks
	private final DiagnosticReportRequestValidator diagnosticReportRequestValidator = new DiagnosticReportRequestValidator();
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test(expected = UnprocessableEntityException.class)
    public void shouldThrowExceptionWhenPendingLabOrderIsInvalid(){
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		Reference reference = new Reference("ServiceRequest");
		reference.setDisplay("Platelet Count");
		List<Reference> basedOn = Collections.singletonList(reference);
		diagnosticReportToCreate.setBasedOn(basedOn);

		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setResults(new HashSet<>(asList(new Obs(), new Obs())));
		fhirDiagnosticReport.setStatus(FhirDiagnosticReport.DiagnosticReportStatus.FINAL);
        fhirDiagnosticReport.setCode(new Concept());
        fhirDiagnosticReport.setSubject(new Patient());
        fhirDiagnosticReport.setResults(new HashSet<>(singletonList(new Obs())));

		when(orderService.getAllOrdersByPatient(any())).thenReturn(Collections.emptyList());

		diagnosticReportRequestValidator.validate(fhirDiagnosticReport);
    }
}
