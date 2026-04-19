/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.o3forms.api.impl.web.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.api.FormService;
import org.openmrs.module.o3forms.service.impl.FormApplicabilityServiceImpl;
import org.openmrs.module.o3forms.web.controller.FormsApplicabilityController;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration-level tests for {@link FormsApplicabilityController}. Extends
 * {@link BaseModuleWebContextSensitiveTest} which loads the full Spring context (including the
 * webservices.rest module) against an in-memory H2 test database seeded with standard OpenMRS test
 * data. NOTE: These tests require the standard OpenMRS test dataset (standardTestDataset.xml) to be
 * on the classpath, which is provided by the openmrs-test artifact.
 */
@ContextConfiguration(locations = { "classpath:TestingApplicationContext.xml" })
@SuppressWarnings("deprecation")
public class FormsApplicabilityControllerTest extends BaseModuleWebContextSensitiveTest {
	
	private static final String ENDPOINT = "/rest/v1/o3forms/forms";
	
	@Autowired
	private WebApplicationContext wac;
	
	@Autowired
	private FormService formService;
	
	private MockMvc mockMvc;
	
	// UUIDs from the standard OpenMRS test dataset
	private static final String MALE_PATIENT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String FEMALE_PATIENT_UUID = "5946f880-b197-400b-9caa-a3c661d23041";
	
	@Before
	public void setUp() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		executeDataSet("org/openmrs/include/standardTestDataset.xml");
		authenticate();
	}
	
	// -------------------------------------------------------------------------
	// AC-1: Endpoint exists and returns 200
	// -------------------------------------------------------------------------
	
	@Test
	public void endpoint_shouldReturn200() throws Exception {
		mockMvc.perform(get(ENDPOINT).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
		        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}
	
	// -------------------------------------------------------------------------
	// AC-3: No patient supplied → all forms returned
	// -------------------------------------------------------------------------
	
	@Test
	public void noPatientParam_shouldReturnAllForms() throws Exception {
		long totalForms = formService.getAllForms(false).size();
		
		mockMvc.perform(get(ENDPOINT).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
		        .andExpect(jsonPath("$", hasSize((int) totalForms)));
	}
	
	// -------------------------------------------------------------------------
	// AC-3: Unknown patient UUID → all forms returned (no filtering)
	// -------------------------------------------------------------------------
	
	@Test
	public void unknownPatientUuid_shouldReturnAllForms() throws Exception {
		long totalForms = formService.getAllForms(false).size();
		
		mockMvc.perform(
		    get(ENDPOINT).param("patient", "00000000-0000-0000-0000-000000000000").accept(MediaType.APPLICATION_JSON))
		        .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize((int) totalForms)));
	}
	
	// -------------------------------------------------------------------------
	// AC-5/6: Forms with applicability rules are filtered correctly
	// -------------------------------------------------------------------------
	
	@Test
	public void formWithTrueRule_shouldBeIncluded() throws Exception {
		Form form = createFormWithApplicabilityRule("Test Form True", "true");
		
		mockMvc.perform(get(ENDPOINT).param("patient", MALE_PATIENT_UUID).accept(MediaType.APPLICATION_JSON))
		        .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.uuid == '" + form.getUuid() + "')]", hasSize(1)));
	}
	
	@Test
	public void formWithFalseRule_shouldBeExcluded() throws Exception {
		Form form = createFormWithApplicabilityRule("Test Form False", "false");
		
		mockMvc.perform(get(ENDPOINT).param("patient", MALE_PATIENT_UUID).accept(MediaType.APPLICATION_JSON))
		        .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.uuid == '" + form.getUuid() + "')]", hasSize(0)));
	}
	
	// -------------------------------------------------------------------------
	// AC-7: Form with no rule is always included
	// -------------------------------------------------------------------------
	
	@Test
	public void formWithNoRule_shouldAlwaysBeIncluded() throws Exception {
		Form form = new Form();
		form.setName("No Rule Form");
		form.setVersion("1");
		formService.saveForm(form);
		
		mockMvc.perform(get(ENDPOINT).param("patient", MALE_PATIENT_UUID).accept(MediaType.APPLICATION_JSON))
		        .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.uuid == '" + form.getUuid() + "')]", hasSize(1)));
	}
	
	// -------------------------------------------------------------------------
	// AC-2/5: Patient gender exposed correctly in SpEL context
	// -------------------------------------------------------------------------
	
	@Test
	public void genderRule_malePatient_shouldIncludeMaleForm() throws Exception {
		Form maleForm = createFormWithApplicabilityRule("Male Only Form", "#patient?.gender == 'M'");
		
		mockMvc.perform(get(ENDPOINT).param("patient", MALE_PATIENT_UUID).accept(MediaType.APPLICATION_JSON))
		        .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.uuid == '" + maleForm.getUuid() + "')]", hasSize(1)));
	}
	
	@Test
	public void genderRule_femalePatient_shouldExcludeMaleForm() throws Exception {
		Form maleForm = createFormWithApplicabilityRule("Male Only Form 2", "#patient?.gender == 'M'");
		
		mockMvc.perform(get(ENDPOINT).param("patient", FEMALE_PATIENT_UUID).accept(MediaType.APPLICATION_JSON))
		        .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.uuid == '" + maleForm.getUuid() + "')]", hasSize(0)));
	}
	
	// -------------------------------------------------------------------------
	// AC-4: Visit UUID exposed in SpEL context
	// -------------------------------------------------------------------------
	
	@Test
	public void visitRule_visitProvided_shouldIncludeForm() throws Exception {
		// Retrieve a visit UUID from the standard test dataset; adjust if your dataset differs.
		String visitUuid = "1e5d5d48-6b78-11e0-93c3-18a905e044dc";
		Form visitForm = createFormWithApplicabilityRule("Visit Required Form", "#visit != null");
		
		mockMvc.perform(get(ENDPOINT).param("visit", visitUuid).accept(MediaType.APPLICATION_JSON))
		        .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.uuid == '" + visitForm.getUuid() + "')]", hasSize(1)));
	}
	
	@Test
	public void visitRule_noVisit_shouldExcludeForm() throws Exception {
		Form visitForm = createFormWithApplicabilityRule("Visit Required Form 2", "#visit != null");
		
		mockMvc.perform(get(ENDPOINT).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
		        .andExpect(jsonPath("$[?(@.uuid == '" + visitForm.getUuid() + "')]", hasSize(0)));
	}
	
	// -------------------------------------------------------------------------
	// Helper
	// -------------------------------------------------------------------------
	
	private Form createFormWithApplicabilityRule(String formName, String spelExpression) {
		Form form = new Form();
		form.setName(formName);
		form.setVersion("1");
		formService.saveForm(form);
		
		FormResource resource = new FormResource();
		resource.setName(FormApplicabilityServiceImpl.APPLICABILITY_RESOURCE_NAME);
		resource.setForm(form);
		resource.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
		resource.setValueReferenceInternal(spelExpression);
		formService.saveFormResource(resource);
		
		return form;
	}
}
