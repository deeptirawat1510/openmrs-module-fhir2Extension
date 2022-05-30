package org.openmrs.module.fhirExtension.validators;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;

import java.util.HashSet;

import static java.util.Collections.singletonList;
import static org.openmrs.module.fhirExtension.validators.DiagnosticReportObsValidator.INVALID_CODE_ERROR_MESSAGE;
import static org.openmrs.module.fhirExtension.validators.DiagnosticReportObsValidator.INVALID_PATIENT_ERROR_MESSAGE;
import static org.openmrs.module.fhirExtension.validators.DiagnosticReportObsValidator.INVALID_RESULTS_ERROR_MESSAGE;
import static org.openmrs.module.fhirExtension.validators.DiagnosticReportObsValidator.INVALID_STATUS_ERROR_MESSAGE;

public class DiagnosticReportObsValidatorTest {
	
	private final DiagnosticReportObsValidator diagnosticReportObsValidator = new DiagnosticReportObsValidator();
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Test
	public void shouldFailWhenStatusIsNotThere() throws Exception {
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		thrown.expect(UnprocessableEntityException.class);
		thrown.expectMessage(INVALID_STATUS_ERROR_MESSAGE);
		
		diagnosticReportObsValidator.validate(fhirDiagnosticReport);
	}
	
	@Test
	public void shouldFailWhenCodeIsNotThere() throws Exception {
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setStatus(FhirDiagnosticReport.DiagnosticReportStatus.FINAL);
		thrown.expect(UnprocessableEntityException.class);
		thrown.expectMessage(INVALID_CODE_ERROR_MESSAGE);
		
		diagnosticReportObsValidator.validate(fhirDiagnosticReport);
	}
	
	@Test
	public void shouldFailWhenSubjectIsNotThere() throws Exception {
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setStatus(FhirDiagnosticReport.DiagnosticReportStatus.FINAL);
		fhirDiagnosticReport.setCode(new Concept());
		thrown.expect(UnprocessableEntityException.class);
		thrown.expectMessage(INVALID_PATIENT_ERROR_MESSAGE);
		
		diagnosticReportObsValidator.validate(fhirDiagnosticReport);
	}
	
	@Test
	public void shouldFailWhenResultsAreEmpty() throws Exception {
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setStatus(FhirDiagnosticReport.DiagnosticReportStatus.FINAL);
		fhirDiagnosticReport.setCode(new Concept());
		fhirDiagnosticReport.setSubject(new Patient());
		thrown.expect(UnprocessableEntityException.class);
		thrown.expectMessage(INVALID_RESULTS_ERROR_MESSAGE);
		
		diagnosticReportObsValidator.validate(fhirDiagnosticReport);
	}
	
	@Test(expected = Test.None.class)
    public void shouldPassWhenStatusCodePatientAndResultsAreThere(){
        FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
        fhirDiagnosticReport.setStatus(FhirDiagnosticReport.DiagnosticReportStatus.FINAL);
        fhirDiagnosticReport.setCode(new Concept());
        fhirDiagnosticReport.setSubject(new Patient());
        fhirDiagnosticReport.setResults(new HashSet<>(singletonList(new Obs())));

		diagnosticReportObsValidator.validate(fhirDiagnosticReport);
    }
}
