package org.openmrs.module.fhirExtension.translators;

import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;

import java.util.Set;

public interface DiagnosticReportObsTranslator extends OpenmrsFhirTranslator<FhirDiagnosticReport, DiagnosticReport> {
}