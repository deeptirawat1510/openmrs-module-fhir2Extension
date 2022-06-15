package org.openmrs.module.fhirExtension.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

//@RunWith(MockitoJUnitRunner.class)
public class ObsBasedDiagnosticReportServiceTest {
	
	//	@Mock
	//	private DiagnosticReportObsTranslatorImpl translator;
	//
	//	@Mock
	//	private DiagnosticReportObsValidator validator;
	//
	//	@Mock
	//	private ObsService obsService;
	//
	//	@Mock
	//	private FhirDiagnosticReportDao dao;
	//
	//	@InjectMocks
	//	private final ObsBasedDiagnosticReportService obsBasedDiagnosticReportService = new ObsBasedDiagnosticReportService();
	
	//	@Test
	//    public void shouldSaveObsIfPresentBeforeSavingDiagnosticReportWhenDiagnosticReportIsCreated() {
	//        DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
	//        FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
	//        fhirDiagnosticReport.setResults(new HashSet<>(asList(new Obs(), new Obs())));
	//
	//        when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
	//        doNothing().when(validator).validate(fhirDiagnosticReport);
	//        FhirDiagnosticReport updatedDiagnosticReport = new FhirDiagnosticReport();
	//        when(dao.createOrUpdate(fhirDiagnosticReport)).thenReturn(updatedDiagnosticReport);
	//        DiagnosticReport diagnosticReportCreated = new DiagnosticReport();
	//        when(translator.toFhirResource(updatedDiagnosticReport)).thenReturn(diagnosticReportCreated);
	//
	//        DiagnosticReport result = obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
	//
	//        assertEquals(diagnosticReportCreated, result);
	//        verify(obsService, times(2)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
	//    }
	//
	//	@Test(expected = UnsupportedOperationException.class)
	//    public void shouldNotSaveObsAndDiagnosticReportWhenDiagnosticReportToCreateIsInValid() {
	//        DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
	//        FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
	//        fhirDiagnosticReport.setResults(new HashSet<>(asList(new Obs(), new Obs())));
	//
	//        when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
	//        doThrow(new UnsupportedOperationException()).when(validator).validate(fhirDiagnosticReport);
	//
	//        obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
	//
	//        verify(obsService, times(0)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
	//        verify(dao, times(0)).createOrUpdate(any(FhirDiagnosticReport.class));
	//    }
}
