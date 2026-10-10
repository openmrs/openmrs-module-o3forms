/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.o3forms.web.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.ClobDatatypeStorage;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

public class O3FormsResourceControllerTest extends BaseModuleWebContextSensitiveTest {
	
	private static final String FORM_URL = "/rest/v1/o3/forms/";
	
	private static final String FORM_UUID = "6b4d2c09-cc51-458d-8e7d-fa3538280dbe";
	
	private static final String FORM_SCHEMA_CLOB_UUID = "0f61245a-b110-4b8a-b8b2-6c7515a61639";
	
	// WEIGHT (KG) in the standard test data, referenced by the first question of the form
	private static final String WEIGHT_CONCEPT_UUID = "c607c80f-1ea9-4da3-bb88-6276ce8868dd";
	
	@Autowired
	private WebApplicationContext webApplicationContext;
	
	private MockMvc mockMvc;
	
	@BeforeEach
	public void setup() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		
		String jsonForm = IOUtils.resourceToString("/forms/test-schemas/concept-references/form.json",
		    StandardCharsets.UTF_8);
		ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
		datatypeStorage.setValue(jsonForm);
		datatypeStorage.setUuid(FORM_SCHEMA_CLOB_UUID);
		Context.getDatatypeService().saveClobDatatypeStorage(datatypeStorage);
		
		Form form = new Form();
		form.setName("form");
		form.setVersion("1.0");
		form.setUuid(FORM_UUID);
		Context.getFormService().saveForm(form);
		
		FormResource formSchemaResource = new FormResource();
		formSchemaResource.setName("JSON schema");
		formSchemaResource.setForm(form);
		formSchemaResource.setValueReferenceInternal(FORM_SCHEMA_CLOB_UUID);
		Context.getFormService().saveFormResource(formSchemaResource);
	}
	
	@Test
	public void getO3Form_shouldReturnNotFoundForAnUnknownForm() throws Exception {
		mockMvc.perform(get(FORM_URL + "no-such-form").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
	}
	
	@Test
	public void getO3Form_shouldIncludeConceptReferencesByDefault() throws Exception {
		// act
		JsonNode result = getForm(get(FORM_URL + FORM_UUID));
		
		// assert
		assertThat(result.path("uuid").asText(), equalTo(FORM_UUID));
		assertThat(result.path("conceptReferences").path(WEIGHT_CONCEPT_UUID).path("display").asText(),
		    equalTo("WEIGHT (KG)"));
	}
	
	@Test
	public void getO3Form_shouldOmitConceptReferencesWhenIncludeConceptReferencesIsFalse() throws Exception {
		// act
		JsonNode result = getForm(get(FORM_URL + FORM_UUID).param("includeConceptReferences", "false"));
		
		// assert
		assertThat(result.path("uuid").asText(), equalTo(FORM_UUID));
		assertThat(result.has("conceptReferences"), is(false));
	}
	
	@Test
	public void getO3Form_shouldUseTheRequestedRepresentationForConceptReferences() throws Exception {
		// act
		JsonNode result = getForm(get(FORM_URL + FORM_UUID).param("v", "custom:(uuid)"));
		
		// assert
		JsonNode weight = result.path("conceptReferences").path(WEIGHT_CONCEPT_UUID);
		assertThat(weight.path("uuid").asText(), equalTo(WEIGHT_CONCEPT_UUID));
		assertThat(weight.has("display"), is(false));
	}
	
	private JsonNode getForm(MockHttpServletRequestBuilder request) throws Exception {
		String content = mockMvc.perform(request.accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn()
		        .getResponse().getContentAsString();
		return new ObjectMapper().readTree(content);
	}
}
