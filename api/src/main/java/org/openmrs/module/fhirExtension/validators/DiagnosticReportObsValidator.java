package org.openmrs.module.fhirExtension.validators;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.apache.commons.lang3.tuple.Pair;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.tuple.Pair.of;
import static org.openmrs.module.fhir2.api.util.FhirUtils.createExceptionErrorOperationOutcome;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
public class DiagnosticReportObsValidator {
	
	static final String INVALID_STATUS_ERROR_MESSAGE = "Invalid status: Should be one from [REGISTERED,PARTIAL,PRELIMINARY,FINAL,UNKNOWN]";
	
	static final String INVALID_CODE_ERROR_MESSAGE = "Invalid test Code";
	
	static final String INVALID_PATIENT_ERROR_MESSAGE = "Invalid patient info";
	
	static final String INVALID_RESULTS_ERROR_MESSAGE = "No test results are there";
	
	private final List<Pair<Predicate<FhirDiagnosticReport>, String>> validations;
	
	public DiagnosticReportObsValidator() {
        validations = asList(
                of(fhirDiagnosticReport -> isNull(fhirDiagnosticReport.getStatus()), INVALID_STATUS_ERROR_MESSAGE),
                of(fhirDiagnosticReport -> isNull(fhirDiagnosticReport.getCode()), INVALID_CODE_ERROR_MESSAGE),
                of(fhirDiagnosticReport -> isNull(fhirDiagnosticReport.getSubject()), INVALID_PATIENT_ERROR_MESSAGE),
                of(fhirDiagnosticReport -> isEmpty(fhirDiagnosticReport.getResults()), INVALID_RESULTS_ERROR_MESSAGE)
        );
    }
	
	public void validate(FhirDiagnosticReport fhirDiagnosticReport) {
        validations.forEach((validation) -> {
            if (validation.getLeft().test(fhirDiagnosticReport)) {
                String errorMessage = validation.getRight();
                throw new UnprocessableEntityException(errorMessage, createExceptionErrorOperationOutcome(errorMessage));
            }
        });
    }
}
