/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.o3forms.api.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.api.DatatypeService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.ClobDatatypeStorage;
import org.openmrs.module.o3forms.api.O3FormsService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.aop.framework.Advised;

public class O3FormsServiceTest extends BaseModuleWebContextSensitiveTest {
	
	private static final String FORM_UUID = "6b4d2c09-cc51-458d-8e7d-fa3538280dbe";
	
	private static final String ADULT_RETURN_CLOB_UUID = "83e6cd2a-8b2e-402e-a00c-7d80a66ccf38";
	
	private static final String COMPONENT_ART_CLOB_UUID = "b115df0d-3573-4611-bf63-de5d2fae2230";
	
	private static final String COMPONENT_HOSPITALIZATION_CLOB_UUID = "99ae5eb4-5865-47eb-9032-4d43885d4a2a";
	
	private static final String COMPONENT_PRECLINIC_REVIEW_CLOB_UUID = "f305e60b-9ce8-45a4-a430-8b2b878f3061";
	
	private static final String SUBFORM_ROOT_FORM_CLOB_UUID = "75d692f0-e83b-4cd3-8962-6c1af6e4f7ce";
	
	private static final String SUBFORM_REFERENCE_FORM_CLOB_UUID = "5f1fa423-56ec-414b-9091-017c44395b27";
	
	private static final String PAGE_REF_ROOT_FORM_CLOB_UUID = "78b01272-cb05-4668-8986-96912ba737a5";
	
	private static final String PAGE_REF_REFERENCED_FORM_CLOB_UUID = "87ec464e-031b-4650-b37f-58484393f5ac";
	
	private static final String SECTION_REF_ROOT_FORM_CLOB_UUID = "78b01272-cb05-4668-8986-96912ba737a5";
	
	private static final String SECTION_REF_REFERENCED_FORM_CLOB_UUID = "20556174-85fe-4255-8492-ce503156f649";
	
	private static final String QUESTION_REF_ROOT_FORM_CLOB_UUID = "2bcbef7e-df52-4498-90a5-f643675b3496";
	
	private static final String QUESTION_REF_REFERENCED_FORM_CLOB_UUID = "592ae63c-4b92-4791-bdee-d678adf94738";
	
	private static final String EXCLUSION_ROOT_FORM_CLOB_UUID = "e66521e2-2a6b-490e-bc40-3c6ee32a57f4";
	
	private static final String EXCLUSION_REFERENCED_FORM_CLOB_UUID = "6db2be71-1d90-4cfc-9be8-2cccdf982638";
	
	private static final String SOLO_FORM_CLOB_UUID = "46357194-2155-48c4-a1b1-6c836ba3bc76";
	
	private static final String SOLO_FORM_TRANSLATIONS_EN_CLOB_UUID = "4229b875-c449-486f-9b5b-528d7264d2a4";
	
	private static final String ROOT_FORM_CLOB_UUID = "0f61245a-b110-4b8a-b8b2-6c7515a61639";
	
	private static final String ROOT_FORM_TRANSLATIONS_EN_CLOB_UUID = "c80e086e-59c6-4ed4-9459-2efe05b99505";
	
	private static final String REFERENCED_FORM_TRANSLATIONS_EN_CLOB_UUID = "24ccfd96-c022-4c42-9019-1f026a4b3ab6";
	
	private static final String FORM_BY_UNAMBIGUOUS_NAME_NAME = "Form2";
	
	private static final String FORM_BY_AMBIGUOUS_NAME_UNRETIRED_PUBLISHED_NAME = "Form3";
	
	private static final String FORM_BY_AMBIGUOUS_NAME_UNRETIRED_PUBLISHED_UUID = "a36585eb-46b7-4ca6-b53d-194e07681254";
	
	private static final String FORM_BY_AMBIGUOUS_NAME_UNRETIRED_UNPUBLISHED_NAME = "Form4";
	
	private static final String FORM_BY_AMBIGUOUS_NAME_UNRETIRED_UNPUBLISHED_UUID = "49bb16b4-c836-43a2-93e5-e9d4228611c1";
	
	private static final String FORM_BY_AMBIGUOUS_NAME_RETIRED_PUBLISHED_NAME = "Form5";
	
	private static final String FORM_BY_AMBIGUOUS_NAME_RETIRED_PUBLISHED_UUID = "cdc3c7a6-c290-4bbb-8bff-e3d4730443bf";
	
	private static final String FORM_BY_AMBIGUOUS_NAME_RETIRED_UNPUBLISHED_NAME = "Form6";
	
	private static final String FORM_BY_AMBIGUOUS_NAME_RETIRED_UNPUBLISHED_UUID = "20ea5282-41d9-4a03-a397-11dae4365fdc";
	
	private static final String FORM_BY_UUID_UUID = "b569cbff-b361-41f7-bba4-0b70d6b71316";
	
	O3FormsService o3FormsService;
	
	@Before
	public void setup() {
		o3FormsService = Context.getRegisteredComponent("o3forms.O3FormsService", O3FormsService.class);
	}
	
	@Test
	public void compile_shouldCombineComponents() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/adult-return/adult-return.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(ADULT_RETURN_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("ampath_poc_adult_return_visit_form_v1.0");
			form.setVersion("1.0");
			form.setUuid(FORM_UUID);
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(ADULT_RETURN_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/adult-return/component-art.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(COMPONENT_ART_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("component_art");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(COMPONENT_ART_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/adult-return/component-hospitalization.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(COMPONENT_HOSPITALIZATION_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("component_hospitalization");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(COMPONENT_HOSPITALIZATION_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/adult-return/component-preclinic-review.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(COMPONENT_PRECLINIC_REVIEW_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("component_preclinic-review");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(COMPONENT_PRECLINIC_REVIEW_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		// act
		SimpleObject result = o3FormsService.compileFormSchema("ampath_poc_adult_return_visit_form_v1.0");
		
		// assert
		assertThat(result, notNullValue());
		
		String referenceCompiledVersion = IOUtils
		        .resourceToString("/forms/test-schemas/adult-return/adult-return-compiled.json", StandardCharsets.UTF_8);
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.valueToTree(result), equalTo(objectMapper.readTree(referenceCompiledVersion)));
	}
	
	@Test
	public void compile_shouldAddSubformsToPages() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/subform/root-form.json", StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(SUBFORM_ROOT_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("root_form");
			form.setVersion("1.0");
			form.setUuid(FORM_UUID);
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(SUBFORM_ROOT_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/subform/subform.json", StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(SUBFORM_REFERENCE_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("subform");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(SUBFORM_REFERENCE_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		// act
		SimpleObject result = o3FormsService.compileFormSchema("root_form");
		
		// assert
		assertThat(result, notNullValue());
		
		String referenceCompiledVersion = IOUtils.resourceToString("/forms/test-schemas/subform/root-form-compiled.json",
		    StandardCharsets.UTF_8);
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.valueToTree(result), equalTo(objectMapper.readTree(referenceCompiledVersion)));
	}
	
	@Test
	public void compile_shouldCombineReferencedPages() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/page-reference/root-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(PAGE_REF_ROOT_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("root_form");
			form.setVersion("1.0");
			form.setUuid(FORM_UUID);
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(PAGE_REF_ROOT_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/page-reference/referenced-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(PAGE_REF_REFERENCED_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("referenced_form");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(PAGE_REF_REFERENCED_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		// act
		SimpleObject result = o3FormsService.compileFormSchema("root_form");
		
		// assert
		assertThat(result, notNullValue());
		
		String referenceCompiledVersion = IOUtils
		        .resourceToString("/forms/test-schemas/page-reference/root-form-compiled.json", StandardCharsets.UTF_8);
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.valueToTree(result), equalTo(objectMapper.readTree(referenceCompiledVersion)));
	}
	
	@Test
	public void compile_shouldCombineReferencedSections() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/section-reference/root-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(SECTION_REF_ROOT_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("root_form");
			form.setVersion("1.0");
			form.setUuid(FORM_UUID);
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(SECTION_REF_ROOT_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/section-reference/referenced-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(SECTION_REF_REFERENCED_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("referenced_form");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(SECTION_REF_REFERENCED_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		// act
		SimpleObject result = o3FormsService.compileFormSchema("root_form");
		
		// assert
		assertThat(result, notNullValue());
		
		String referenceCompiledVersion = IOUtils
		        .resourceToString("/forms/test-schemas/section-reference/root-form-compiled.json", StandardCharsets.UTF_8);
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.valueToTree(result), equalTo(objectMapper.readTree(referenceCompiledVersion)));
	}
	
	@Test
	public void compile_shouldCombineReferencedQuestions() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/question-reference/root-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(QUESTION_REF_ROOT_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("root_form");
			form.setVersion("1.0");
			form.setUuid(FORM_UUID);
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(QUESTION_REF_ROOT_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/question-reference/referenced-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(QUESTION_REF_REFERENCED_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("referenced_form");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(QUESTION_REF_REFERENCED_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		// act
		SimpleObject result = o3FormsService.compileFormSchema("root_form");
		
		// assert
		assertThat(result, notNullValue());
		
		String referenceCompiledVersion = IOUtils
		        .resourceToString("/forms/test-schemas/question-reference/root-form-compiled.json", StandardCharsets.UTF_8);
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.valueToTree(result), equalTo(objectMapper.readTree(referenceCompiledVersion)));
	}
	
	@Test
	public void compile_shouldCombineReferencedQuestionsFromObsGroups() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/obsgroup-reference/root-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(QUESTION_REF_ROOT_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("root_form");
			form.setVersion("1.0");
			form.setUuid(FORM_UUID);
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(QUESTION_REF_ROOT_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/obsgroup-reference/referenced-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(QUESTION_REF_REFERENCED_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("referenced_form");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(QUESTION_REF_REFERENCED_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		// act
		SimpleObject result = o3FormsService.compileFormSchema("root_form");
		
		// assert
		assertThat(result, notNullValue());
		
		String referenceCompiledVersion = IOUtils
		        .resourceToString("/forms/test-schemas/obsgroup-reference/root-form-compiled.json", StandardCharsets.UTF_8);
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.valueToTree(result), equalTo(objectMapper.readTree(referenceCompiledVersion)));
	}
	
	@Test
	public void compile_shouldExcludeQuestionsDefinedInPageReference() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/exclusions/root-form-page-exclusion.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(EXCLUSION_ROOT_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("root_form");
			form.setVersion("1.0");
			form.setUuid(FORM_UUID);
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(EXCLUSION_ROOT_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/exclusions/referenced-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(EXCLUSION_REFERENCED_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("referenced_form");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(EXCLUSION_REFERENCED_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		// act
		SimpleObject result = o3FormsService.compileFormSchema("root_form");
		
		// assert
		assertThat(result, notNullValue());
		
		String referenceCompiledVersion = IOUtils
		        .resourceToString("/forms/test-schemas/exclusions/root-form-page-compiled.json", StandardCharsets.UTF_8);
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.valueToTree(result), equalTo(objectMapper.readTree(referenceCompiledVersion)));
	}
	
	@Test
	public void compile_shouldExcludeQuestionsDefinedInSectionReference() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/exclusions/root-form-section-exclusion.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(EXCLUSION_ROOT_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("root_form");
			form.setVersion("1.0");
			form.setUuid(FORM_UUID);
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(EXCLUSION_ROOT_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/exclusions/referenced-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(EXCLUSION_REFERENCED_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("referenced_form");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(EXCLUSION_REFERENCED_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		// act
		SimpleObject result = o3FormsService.compileFormSchema("root_form");
		
		// assert
		assertThat(result, notNullValue());
		
		String referenceCompiledVersion = IOUtils
		        .resourceToString("/forms/test-schemas/exclusions/root-form-section-compiled.json", StandardCharsets.UTF_8);
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.valueToTree(result), equalTo(objectMapper.readTree(referenceCompiledVersion)));
	}
	
	@Test
	public void compile_shouldAddTheEncounterTypeDataIfFormHasAssociatedEncounterType() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		EncounterService encounterService = Context.getEncounterService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/encountertype/root-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(ROOT_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			Form form = new Form();
			form.setName("root_form");
			form.setVersion("1.0");
			form.setUuid(FORM_UUID);
			form.setEncounterType(encounterService.getEncounterTypeByUuid("61ae96f4-6afe-4351-b6f8-cd4fc383cce1"));
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(ROOT_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
		}
		
		// act
		SimpleObject result = o3FormsService.compileFormSchema("root_form");
		
		// assert
		assertThat(result, notNullValue());
		
		String referenceCompiledVersion = IOUtils
		        .resourceToString("/forms/test-schemas/encountertype/root-form-compiled.json", StandardCharsets.UTF_8);
		ObjectMapper objectMapper = new ObjectMapper();
		assertThat(objectMapper.valueToTree(result), equalTo(objectMapper.readTree(referenceCompiledVersion)));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void getConceptReferences_shouldLoadConceptReferencesForCompiledForm() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		String jsonForm = IOUtils.resourceToString("/forms/test-schemas/concept-references/form.json",
		    StandardCharsets.UTF_8);
		ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
		datatypeStorage.setValue(jsonForm);
		datatypeStorage.setUuid(ROOT_FORM_CLOB_UUID);
		datatypeService.saveClobDatatypeStorage(datatypeStorage);
		
		Form form = new Form();
		form.setName("form");
		form.setVersion("1.0");
		formService.saveForm(form);
		
		FormResource formSchemaResource = new FormResource();
		formSchemaResource.setName("JSON schema");
		formSchemaResource.setForm(form);
		formSchemaResource.setValueReferenceInternal(ROOT_FORM_CLOB_UUID);
		formService.saveFormResource(formSchemaResource);
		
		SimpleObject formSchema = SimpleObject.parseJson(jsonForm);
		
		// act
		SimpleObject result = o3FormsService.getConceptReferences(formSchema);
		
		// assert
		assertThat(result, notNullValue());
		// all concepts defined in the standard test data
		assertThat(result, hasKey("c607c80f-1ea9-4da3-bb88-6276ce8868dd"));
		assertThat((Map<String, String>) result.get("c607c80f-1ea9-4da3-bb88-6276ce8868dd"),
		    allOf(hasEntry("uuid", "c607c80f-1ea9-4da3-bb88-6276ce8868dd"), hasEntry("display", "WEIGHT (KG)")));
		assertThat(result, hasKey("a09ab2c5-878e-4905-b25d-5784167d0216"));
		assertThat((Map<String, String>) result.get("a09ab2c5-878e-4905-b25d-5784167d0216"),
		    allOf(hasEntry("uuid", "a09ab2c5-878e-4905-b25d-5784167d0216"), hasEntry("display", "CD4 COUNT")));
		assertThat(result, hasKey("b055abd8-a420-4a11-8b98-02ee170a7b54"));
		assertThat((Map<String, String>) result.get("b055abd8-a420-4a11-8b98-02ee170a7b54"),
		    allOf(hasEntry("uuid", "b055abd8-a420-4a11-8b98-02ee170a7b54"), hasEntry("display", "YES")));
		assertThat(result, hasKey("934d8ef1-ea43-4f98-906e-dd03d5faaeb4"));
		assertThat((Map<String, String>) result.get("934d8ef1-ea43-4f98-906e-dd03d5faaeb4"),
		    allOf(hasEntry("uuid", "934d8ef1-ea43-4f98-906e-dd03d5faaeb4"), hasEntry("display", "NO")));
		assertThat(result, hasKey("6e02d1a0-7869-11e4-981f-0800200c9a89"));
		assertThat((Map<String, String>) result.get("6e02d1a0-7869-11e4-981f-0800200c9a89"),
		    allOf(hasEntry("uuid", "6e02d1a0-7869-11e4-981f-0800200c9a89"), hasEntry("display", "Drug Routes")));
		assertThat(result, hasKey("e10ffe54-5184-4efe-8960-cd565ec1cdf8"));
		assertThat((Map<String, String>) result.get("e10ffe54-5184-4efe-8960-cd565ec1cdf8"),
		    allOf(hasEntry("uuid", "e10ffe54-5184-4efe-8960-cd565ec1cdf8"), hasEntry("display", "UNKNOWN")));
	}
	
	@Test
	public void getTranslations_shouldLoadTranslationsForForm() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/translations/solo-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(SOLO_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			String jsonTranslations = IOUtils.resourceToString(
			    "/forms/test-schemas/translations/solo-form-translations-en.json", StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorageTranslations = new ClobDatatypeStorage();
			datatypeStorageTranslations.setValue(jsonTranslations);
			datatypeStorageTranslations.setUuid(SOLO_FORM_TRANSLATIONS_EN_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorageTranslations);
			
			Form form = new Form();
			form.setName("solo_form");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(SOLO_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
			
			FormResource translations = new FormResource();
			translations.setName("solo_form_translations_en");
			translations.setForm(form);
			translations.setValueReferenceInternal(SOLO_FORM_TRANSLATIONS_EN_CLOB_UUID);
			formService.saveFormResource(translations);
		}
		
		// act
		Map<String, ?> translations = o3FormsService.getTranslations("solo_form");
		
		// assert
		assertThat(translations, notNullValue());
		assertThat(translations, hasEntry("key1", "Translation 1"));
		assertThat(translations, hasEntry("key2", "Translation 2"));
	}
	
	@Test
	public void getTranslations_shouldLoadTranslationsFromReferencedForm() throws Exception {
		// arrange
		DatatypeService datatypeService = Context.getDatatypeService();
		FormService formService = Context.getFormService();
		
		{
			String jsonForm = IOUtils.resourceToString("/forms/test-schemas/translations/root-form.json",
			    StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorage = new ClobDatatypeStorage();
			datatypeStorage.setValue(jsonForm);
			datatypeStorage.setUuid(ROOT_FORM_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorage);
			
			String jsonTranslations = IOUtils.resourceToString(
			    "/forms/test-schemas/translations/root-form-translations-en.json", StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorageTranslations = new ClobDatatypeStorage();
			datatypeStorageTranslations.setValue(jsonTranslations);
			datatypeStorageTranslations.setUuid(ROOT_FORM_TRANSLATIONS_EN_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorageTranslations);
			
			Form form = new Form();
			form.setName("root_form");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource formSchema = new FormResource();
			formSchema.setName("JSON schema");
			formSchema.setForm(form);
			formSchema.setValueReferenceInternal(ROOT_FORM_CLOB_UUID);
			formService.saveFormResource(formSchema);
			
			FormResource translations = new FormResource();
			translations.setName("solo_form_translations_en");
			translations.setForm(form);
			translations.setValueReferenceInternal(ROOT_FORM_TRANSLATIONS_EN_CLOB_UUID);
			formService.saveFormResource(translations);
		}
		{
			String jsonTranslations = IOUtils.resourceToString(
			    "/forms/test-schemas/translations/referenced-form-translations-en.json", StandardCharsets.UTF_8);
			ClobDatatypeStorage datatypeStorageTranslations = new ClobDatatypeStorage();
			datatypeStorageTranslations.setValue(jsonTranslations);
			datatypeStorageTranslations.setUuid(REFERENCED_FORM_TRANSLATIONS_EN_CLOB_UUID);
			datatypeService.saveClobDatatypeStorage(datatypeStorageTranslations);
			
			Form form = new Form();
			form.setName("referenced_form");
			form.setVersion("1.0");
			formService.saveForm(form);
			
			FormResource translations = new FormResource();
			translations.setName("solo_form_translations_en");
			translations.setForm(form);
			translations.setValueReferenceInternal(REFERENCED_FORM_TRANSLATIONS_EN_CLOB_UUID);
			formService.saveFormResource(translations);
		}
		
		// act
		Map<String, ?> translations = o3FormsService.getTranslations("root_form");
		
		// assert
		assertThat(translations, notNullValue());
		assertThat(translations, hasEntry("key1", "Translation 1"));
		assertThat(translations, hasEntry("key2", "Translation 2"));
		assertThat(translations, hasEntry("key3", "Translation 3"));
		assertThat(translations, hasEntry("key4", "Translation 4"));
	}
	
	@Test
	public void getForm_shouldGetFormByUuid() throws Exception {
		// arrange
		FormService formService = Context.getFormService();
		
		{
			Form form = new Form();
			form.setName("Form1");
			form.setVersion("1.0");
			form.setUuid(FORM_BY_UUID_UUID);
			formService.saveForm(form);
		}
		
		// act
		Form form = ((O3FormsServiceImpl) unproxyTarget(o3FormsService)).getForm(FORM_BY_UUID_UUID);
		
		// assert
		assertThat(form, notNullValue());
		assertThat(form.getName(), equalTo("Form1"));
		assertThat(form.getUuid(), equalTo(FORM_BY_UUID_UUID));
	}
	
	@Test
	public void getForm_shouldGetFormByName() throws Exception {
		// arrange
		FormService formService = Context.getFormService();
		
		{
			Form form = new Form();
			form.setName(FORM_BY_UNAMBIGUOUS_NAME_NAME);
			form.setVersion("1.0");
			formService.saveForm(form);
		}
		
		// act
		Form form = ((O3FormsServiceImpl) unproxyTarget(o3FormsService)).getForm(FORM_BY_UNAMBIGUOUS_NAME_NAME);
		
		// assert
		assertThat(form, notNullValue());
		assertThat(form.getName(), equalTo(FORM_BY_UNAMBIGUOUS_NAME_NAME));
	}
	
	@Test
	public void getForm_shouldGetActivePublishedFormByName() throws Exception {
		// arrange
		FormService formService = Context.getFormService();
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_PUBLISHED_NAME);
			form.setVersion("1.0");
			form.setRetired(true);
			form.setRetireReason("Some reason");
			formService.saveForm(form);
		}
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_PUBLISHED_NAME);
			form.setVersion("2.0");
			form.setRetired(false);
			form.setPublished(false);
			formService.saveForm(form);
		}
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_PUBLISHED_NAME);
			form.setVersion("3.0");
			form.setRetired(false);
			form.setPublished(true);
			form.setUuid(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_PUBLISHED_UUID);
			formService.saveForm(form);
		}
		
		// act
		Form form = ((O3FormsServiceImpl) unproxyTarget(o3FormsService))
		        .getForm(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_PUBLISHED_NAME);
		
		// assert
		assertThat(form, notNullValue());
		assertThat(form.getName(), equalTo(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_PUBLISHED_NAME));
		assertThat(form.getRetired(), is(false));
		assertThat(form.getPublished(), is(true));
		assertThat(form.getUuid(), equalTo(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_PUBLISHED_UUID));
	}
	
	@Test
	public void getForm_shouldGetActiveUnpublishedFormByName() throws Exception {
		// arrange
		FormService formService = Context.getFormService();
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_UNPUBLISHED_NAME);
			form.setVersion("1.0");
			form.setRetired(true);
			form.setRetireReason("Some reason");
			formService.saveForm(form);
		}
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_UNPUBLISHED_NAME);
			form.setVersion("2.0");
			form.setRetired(false);
			form.setPublished(false);
			form.setUuid(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_UNPUBLISHED_UUID);
			formService.saveForm(form);
		}
		
		// act
		Form form = ((O3FormsServiceImpl) unproxyTarget(o3FormsService))
		        .getForm(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_UNPUBLISHED_NAME);
		
		// assert
		assertThat(form, notNullValue());
		assertThat(form.getName(), equalTo(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_UNPUBLISHED_NAME));
		assertThat(form.getRetired(), is(false));
		assertThat(form.getPublished(), is(false));
		assertThat(form.getUuid(), equalTo(FORM_BY_AMBIGUOUS_NAME_UNRETIRED_UNPUBLISHED_UUID));
	}
	
	@Test
	public void getForm_shouldGetInactivePublishedFormByName() throws Exception {
		// arrange
		FormService formService = Context.getFormService();
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_RETIRED_PUBLISHED_NAME);
			form.setVersion("1.0");
			form.setRetired(true);
			form.setRetireReason("Some reason");
			formService.saveForm(form);
			Thread.sleep(1000); // unfortunately, we need to pause for 1 second so that the dates between
			                    // versions of this form are different
		}
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_RETIRED_PUBLISHED_NAME);
			form.setVersion("2.0");
			form.setRetired(true);
			form.setRetireReason("Some reason");
			form.setPublished(true);
			formService.saveForm(form);
			Thread.sleep(1000);
		}
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_RETIRED_PUBLISHED_NAME);
			form.setVersion("3.0");
			form.setRetired(true);
			form.setRetireReason("Some reason");
			form.setPublished(true);
			form.setUuid(FORM_BY_AMBIGUOUS_NAME_RETIRED_PUBLISHED_UUID);
			formService.saveForm(form);
		}
		
		// act
		Form form = ((O3FormsServiceImpl) unproxyTarget(o3FormsService))
		        .getForm(FORM_BY_AMBIGUOUS_NAME_RETIRED_PUBLISHED_NAME);
		
		// assert
		assertThat(form, notNullValue());
		assertThat(form.getName(), equalTo(FORM_BY_AMBIGUOUS_NAME_RETIRED_PUBLISHED_NAME));
		assertThat(form.getRetired(), is(true));
		assertThat(form.getPublished(), is(true));
		assertThat(form.getUuid(), equalTo(FORM_BY_AMBIGUOUS_NAME_RETIRED_PUBLISHED_UUID));
	}
	
	@Test
	public void getForm_shouldGetInactiveFormByName() throws Exception {
		// arrange
		FormService formService = Context.getFormService();
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_RETIRED_UNPUBLISHED_NAME);
			form.setVersion("1.0");
			form.setRetired(true);
			form.setRetireReason("Some reason");
			formService.saveForm(form);
			Thread.sleep(1000); // unfortunately, we need to pause for 1 second so that the dates between
			                    // versions of this form are different
		}
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_RETIRED_UNPUBLISHED_NAME);
			form.setVersion("2.0");
			form.setRetired(true);
			form.setRetireReason("Some reason");
			formService.saveForm(form);
			Thread.sleep(1000);
		}
		
		{
			Form form = new Form();
			form.setName(FORM_BY_AMBIGUOUS_NAME_RETIRED_UNPUBLISHED_NAME);
			form.setVersion("2.0");
			form.setRetired(true);
			form.setRetireReason("Some reason");
			form.setUuid(FORM_BY_AMBIGUOUS_NAME_RETIRED_UNPUBLISHED_UUID);
			formService.saveForm(form);
		}
		
		// act
		Form form = ((O3FormsServiceImpl) unproxyTarget(o3FormsService))
		        .getForm(FORM_BY_AMBIGUOUS_NAME_RETIRED_UNPUBLISHED_NAME);
		
		// assert
		assertThat(form, notNullValue());
		assertThat(form.getName(), equalTo(FORM_BY_AMBIGUOUS_NAME_RETIRED_UNPUBLISHED_NAME));
		assertThat(form.getRetired(), is(true));
		assertThat(form.getPublished(), is(false));
		assertThat(form.getUuid(), equalTo(FORM_BY_AMBIGUOUS_NAME_RETIRED_UNPUBLISHED_UUID));
	}
	
	@SuppressWarnings("unchecked")
	private <T> T unproxyTarget(Object proxy) throws Exception {
		if (proxy instanceof Advised) {
			return unproxyTarget(((Advised) proxy).getTargetSource().getTarget());
		}
		return (T) proxy;
	}
}
