package org.openmrs.module.fhirExtension.service;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirDiagnosticReportDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport;
import org.openmrs.module.fhirExtension.translators.ObsBasedDiagnosticReportTranslator;
import org.openmrs.module.fhirExtension.validators.DiagnosticReportObsValidator;
import org.openmrs.module.fhirExtension.validators.DiagnosticReportRequestValidator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fhirExtension.service.ObsBasedDiagnosticReportService.SAVE_OBS_MESSAGE;

@RunWith(MockitoJUnitRunner.class)
public class ObsBasedDiagnosticReportServiceTest {
	
	@Mock
	private ObsBasedDiagnosticReportTranslator translator;
	
	@Mock
	private DiagnosticReportRequestValidator diagnosticReportRequestValidator;
	
	@Mock
	private DiagnosticReportObsValidator validator;
	
	@Mock
	private ObsService obsService;
	
	@Mock
	private OrderUpdateService orderUpdateService;
	
	@Mock
	private FhirDiagnosticReportDao dao;
	
	@Mock
	private SearchQuery<FhirDiagnosticReport, DiagnosticReport, FhirDiagnosticReportDao, ObsBasedDiagnosticReportTranslator, SearchQueryInclude<DiagnosticReport>> mockSearchQuery;
	
	@Mock
	private SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	
	@InjectMocks
	private final ObsBasedDiagnosticReportService obsBasedDiagnosticReportService = new ObsBasedDiagnosticReportService();
	
	@Test
	public void shouldSaveObsIfPresentBeforeSavingDiagnosticReportWhenDiagnosticReportIsCreated() {
		DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
		FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
		fhirDiagnosticReport.setResults(of(new Obs(), new Obs()).collect(toSet()));
		
		when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
		doNothing().when(validator).validate(fhirDiagnosticReport);
		FhirDiagnosticReport updatedFhirDiagnosticReport = new FhirDiagnosticReport();
		when(dao.createOrUpdate(fhirDiagnosticReport)).thenReturn(updatedFhirDiagnosticReport);
		DiagnosticReport diagnosticReportCreated = new DiagnosticReport();
		doNothing().when(orderUpdateService).updateOrder(diagnosticReportToCreate, fhirDiagnosticReport);
		when(translator.toFhirResource(updatedFhirDiagnosticReport)).thenReturn(diagnosticReportCreated);
		
		DiagnosticReport result = obsBasedDiagnosticReportService.create(diagnosticReportToCreate);
		
		assertEquals(diagnosticReportCreated, result);
		verify(obsService, times(2)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
	}
	
	@Test(expected = UnsupportedOperationException.class)
    public void shouldNotSaveObsAndDiagnosticReportWhenDiagnosticReportToCreateIsInValid() {
        DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
        FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
        fhirDiagnosticReport.setResults(new HashSet<>(asList(new Obs(), new Obs())));

        doNothing().when(diagnosticReportRequestValidator).validate(diagnosticReportToCreate);
        when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
        doThrow(new UnsupportedOperationException()).when(validator).validate(fhirDiagnosticReport);

        obsBasedDiagnosticReportService.create(diagnosticReportToCreate);

        verify(obsService, times(0)).saveObs(any(Obs.class), eq(SAVE_OBS_MESSAGE));
        verify(dao, times(0)).createOrUpdate(any(FhirDiagnosticReport.class));
    }
	
	@Test(expected = UnsupportedOperationException.class)
    public void shouldNotSaveObsAndDiagnosticReportWhenLabTestIsNotAValidPendingOrder() {
        DiagnosticReport diagnosticReportToCreate = new DiagnosticReport();
        FhirDiagnosticReport fhirDiagnosticReport = new FhirDiagnosticReport();
        fhirDiagnosticReport.setResults(new HashSet<>(asList(new Obs(), new Obs())));
        Reference reference = new Reference("ServiceRequest");
        reference.setDisplay("Platelet Count");
        List<Reference> basedOn = Collections.singletonList(reference);
        diagnosticReportToCreate.setBasedOn(basedOn);

        when(translator.toOpenmrsType(diagnosticReportToCreate)).thenReturn(fhirDiagnosticReport);
        doNothing().when(validator).validate(fhirDiagnosticReport);
        doThrow(new UnsupportedOperationException()).when(diagnosticReportRequestValidator).validate(diagnosticReportToCreate);

        obsBasedDiagnosticReportService.create(diagnosticReportToCreate);

        verify(dao, times(0)).createOrUpdate(any(FhirDiagnosticReport.class));
    }
	
	@Test
	public void shouldCallQueryResultsWithProperParamtersWhenSearchNeedsToBePerformedForPatient() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam();
		
		obsBasedDiagnosticReportService.searchForDiagnosticReports(null, patientReference, null, null, null, null, null,
		    null, null);
		
		ArgumentCaptor<SearchParameterMap> searchParameterArgumentCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
		ArgumentCaptor<FhirDiagnosticReportDao> daoArgumentCaptor = ArgumentCaptor.forClass(FhirDiagnosticReportDao.class);
		ArgumentCaptor<ObsBasedDiagnosticReportTranslator> translatorArgumentCaptor = ArgumentCaptor
		        .forClass(ObsBasedDiagnosticReportTranslator.class);
		ArgumentCaptor<SearchQueryInclude> searchQueryIncludeArgumentCaptor = ArgumentCaptor
		        .forClass(SearchQueryInclude.class);
		verify(mockSearchQuery).getQueryResults(searchParameterArgumentCaptor.capture(), daoArgumentCaptor.capture(),
		    translatorArgumentCaptor.capture(), searchQueryIncludeArgumentCaptor.capture());
		
		assertNotNull(searchParameterArgumentCaptor.getValue().getParameters(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER));
		assertEquals(dao, daoArgumentCaptor.getValue());
		assertEquals(translator, translatorArgumentCaptor.getValue());
		assertEquals(searchQueryInclude, searchQueryIncludeArgumentCaptor.getValue());
	}
}
