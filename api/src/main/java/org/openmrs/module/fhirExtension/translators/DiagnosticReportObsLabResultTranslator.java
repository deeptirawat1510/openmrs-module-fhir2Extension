package org.openmrs.module.fhirExtension.translators;

import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.domain.observation.LabResult;

public interface DiagnosticReportObsLabResultTranslator extends OpenmrsFhirTranslator<Obs, LabResult> {}
