package org.openmrs.module.fhirExtension.export.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.FhirConditionService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConditionExportTest {
	
	@Mock
	private FhirConditionService fhirConditionService;
	
	@InjectMocks
	private ConditionExport conditionExport;
	
	@Test
	public void shouldExportConditions_whenValidDateRangeProvided() {
		when(fhirConditionService.searchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(getMockConditionBundle(2));
		
		List<IBaseResource> conditionResources = conditionExport.export("2023-05-01", "2023-05-31");
		
		assertNotNull(conditionResources);
		assertEquals(2, conditionResources.size());
	}
	
	@Test
	public void shouldExportConditions_whenValidStartDateProvided() {
		when(fhirConditionService.searchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(getMockConditionBundle(1));
		
		List<IBaseResource> conditionResources = conditionExport.export("2023-05-01", null);
		
		assertNotNull(conditionResources);
		assertEquals(1, conditionResources.size());
	}
	
	@Test
	public void shouldExportConditions_whenValidEndDateProvided() {
		when(fhirConditionService.searchConditions(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(getMockConditionBundle(1));
		
		List<IBaseResource> conditionResources = conditionExport.export(null, "2023-05-31");
		
		assertNotNull(conditionResources);
		assertEquals(1, conditionResources.size());
	}
	
	private IBundleProvider getMockConditionBundle(int count) {
		Condition activeConditionResource = new Condition();
		CodeableConcept activeClinicalStatus = new CodeableConcept();
		activeClinicalStatus.setCoding(Collections.singletonList(new Coding("dummy", "active", "active")));
		activeConditionResource.setClinicalStatus(activeClinicalStatus);
		Condition inactiveConditionResource = new Condition();
		CodeableConcept inactiveClinicalStatus = new CodeableConcept();
		inactiveClinicalStatus.setCoding(Collections.singletonList(new Coding("dummy", "history", "history")));
		inactiveConditionResource.setClinicalStatus(inactiveClinicalStatus);
		
		IBundleProvider iBundleProvider = null;
		if (count == 1) {
			iBundleProvider = new SimpleBundleProvider(Arrays.asList(activeConditionResource));
		} else {
			iBundleProvider = new SimpleBundleProvider(Arrays.asList(activeConditionResource, inactiveConditionResource));
		}
		
		return iBundleProvider;
	}
}
