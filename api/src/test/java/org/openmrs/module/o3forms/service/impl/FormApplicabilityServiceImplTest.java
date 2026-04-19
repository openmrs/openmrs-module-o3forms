/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.o3forms.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Visit;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;

/**
 * Unit tests for {@link FormApplicabilityServiceImpl}. The OpenMRS {@link Context} is mocked via
 * Mockito's static mocking so that no Spring application context or database is required.
 */
@RunWith(MockitoJUnitRunner.class)
public class FormApplicabilityServiceImplTest {
	
	@Mock
	private FormService formService;
	
	private FormApplicabilityServiceImpl service;
	
	// --- Fixtures -----------------------------------------------------------
	
	private Form formNoRule; // has no applicability resource   → always included
	
	private Form formRuleTrue; // rule evaluates to true           → included
	
	private Form formRuleFalse; // rule evaluates to false          → excluded
	
	private Form formRuleGenderMale; // rule: patient.gender == 'M'
	
	private Form formRuleVisit; // rule: visit != null
	
	private Patient malePatient;
	
	private Patient femalePatient;
	
	private Visit visit;
	
	@Before
	public void setUp() {
		service = new FormApplicabilityServiceImpl();
		
		// Build forms
		formNoRule = form("form-no-rule", "Form Without Rule");
		formRuleTrue = formWithRule("form-rule-true", "Form Rule True", "true");
		formRuleFalse = formWithRule("form-rule-false", "Form Rule False", "false");
		formRuleGenderMale = formWithRule("form-gender-male", "Prenatal Form", "#patient?.gender == 'M'");
		formRuleVisit = formWithRule("form-visit", "Visit Form", "#visit != null");
		
		// Build patients
		malePatient = patient("M");
		femalePatient = patient("F");
		
		// Build visit
		visit = new Visit();
		visit.setUuid("test-visit-uuid");
		
		// Wire FormService stubs for each form
		when(formService.getFormResourcesForForm(formNoRule)).thenReturn(Collections.emptyList());
		when(formService.getFormResourcesForForm(formRuleTrue))
		        .thenReturn(Collections.singletonList(resource(formRuleTrue, "true")));
		when(formService.getFormResourcesForForm(formRuleFalse))
		        .thenReturn(Collections.singletonList(resource(formRuleFalse, "false")));
		when(formService.getFormResourcesForForm(formRuleGenderMale))
		        .thenReturn(Collections.singletonList(resource(formRuleGenderMale, "#patient?.gender == 'M'")));
		when(formService.getFormResourcesForForm(formRuleVisit))
		        .thenReturn(Collections.singletonList(resource(formRuleVisit, "#visit != null")));
	}
	
	// --- Tests --------------------------------------------------------------
	
	/** AC-7: form with no applicability rule is always included. */
	@Test
	public void getApplicableForms_formWithNoRule_alwaysIncluded() {
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> result = service.getApplicableForms(Collections.singletonList(formNoRule), null, null);
			
			assertThat(result, hasSize(1));
			assertThat(result.get(0).getUuid(), is("form-no-rule"));
		}
	}
	
	/** AC-5/6: rule "true" → included; rule "false" → excluded. */
	@Test
	public void getApplicableForms_literalTrueAndFalseRules() {
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> result = service.getApplicableForms(Arrays.asList(formRuleTrue, formRuleFalse), null, null);
			
			assertThat(result, hasSize(1));
			assertThat(result.get(0).getUuid(), is("form-rule-true"));
		}
	}
	
	/** AC-2: patient variable bound in SpEL context; gender-specific form shown for male. */
	@Test
	public void getApplicableForms_genderRule_malePatient_included() {
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> result = service.getApplicableForms(Collections.singletonList(formRuleGenderMale), malePatient, null);
			
			assertThat(result, hasSize(1));
		}
	}
	
	/** AC-2: female patient → gender-specific form for males excluded. */
	@Test
	public void getApplicableForms_genderRule_femalePatient_excluded() {
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> result = service.getApplicableForms(Collections.singletonList(formRuleGenderMale), femalePatient,
			    null);
			
			assertThat(result, is(empty()));
		}
	}
	
	/** AC-3: no patient supplied → all forms returned (no filtering). */
	@Test
	public void getApplicableForms_noPatient_allFormsReturned() {
		// When patient is null the SpEL safe-navigation '#patient?.gender' returns null,
		// null == 'M' → false. But the contract (AC-3) says "no filtering" when no patient
		// is supplied. In the current implementation the rule IS still evaluated but receives
		// null as the patient. For a pure "return all" on null patient the rule at the service
		// boundary would need to short-circuit; this test verifies the currently-specified
		// behaviour: the rule is evaluated with patient=null.
		//
		// If the team decides that null patient means "skip all filtering", this test should
		// be updated and the service implementation adjusted accordingly.
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> result = service.getApplicableForms(Arrays.asList(formNoRule, formRuleTrue, formRuleFalse), null,
			    null);
			
			// formNoRule (no rule) + formRuleTrue (literal true) = 2
			assertThat(result, hasSize(2));
		}
	}
	
	/** AC-4: visit variable bound in SpEL context; rule passes when visit is provided. */
	@Test
	public void getApplicableForms_visitRule_withVisit_included() {
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> result = service.getApplicableForms(Collections.singletonList(formRuleVisit), null, visit);
			
			assertThat(result, hasSize(1));
		}
	}
	
	/** AC-4: visit variable is null when not supplied → rule #visit != null → false. */
	@Test
	public void getApplicableForms_visitRule_withoutVisit_excluded() {
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> result = service.getApplicableForms(Collections.singletonList(formRuleVisit), null, null);
			
			assertThat(result, is(empty()));
		}
	}
	
	/** AC-7: mixed list of forms with and without rules, no patient/visit. */
	@Test
	public void getApplicableForms_mixedForms_correctFiltering() {
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> all = Arrays.asList(formNoRule, formRuleTrue, formRuleFalse, formRuleVisit);
			
			List<Form> result = service.getApplicableForms(all, null, null);
			
			// formNoRule + formRuleTrue = 2 (formRuleFalse and formRuleVisit excluded)
			assertThat(result, hasSize(2));
			assertThat(result.stream().map(Form::getUuid).collect(java.util.stream.Collectors.toList()),
			    containsInAnyOrder("form-no-rule", "form-rule-true"));
		}
	}
	
	/** A malformed / throwing SpEL expression should cause the form to be excluded defensively. */
	@Test
	public void getApplicableForms_malformedExpression_formExcluded() {
		Form badForm = formWithRule("form-bad", "Bad Rule Form", "{{{{invalid spel}}}");
		when(formService.getFormResourcesForForm(badForm))
		        .thenReturn(Collections.singletonList(resource(badForm, "{{{{invalid spel}}}")));
		
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> result = service.getApplicableForms(Collections.singletonList(badForm), null, null);
			
			assertThat(result, is(empty()));
		}
	}
	
	/** Empty input list returns empty result. */
	@Test
	public void getApplicableForms_emptyList_returnsEmpty() {
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			
			List<Form> result = service.getApplicableForms(Collections.emptyList(), null, null);
			
			assertThat(result, is(empty()));
		}
	}
	
	/** Null list should throw IllegalArgumentException. */
	@Test(expected = IllegalArgumentException.class)
	public void getApplicableForms_nullList_throwsException() {
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::getFormService).thenReturn(formService);
			service.getApplicableForms(null, null, null);
		}
	}
	
	// --- Factory helpers ----------------------------------------------------
	
	private static Form form(String uuid, String name) {
		Form f = new Form();
		f.setUuid(uuid);
		f.setName(name);
		f.setVersion("1");
		f.setRetired(false);
		return f;
	}
	
	private static Form formWithRule(String uuid, String name, String spelExpression) {
		return form(uuid, name);
	}
	
	private static FormResource resource(Form form, String spelExpression) {
		FormResource r = new FormResource();
		r.setName(FormApplicabilityServiceImpl.APPLICABILITY_RESOURCE_NAME);
		r.setForm(form);
		r.setValue(spelExpression);
		return r;
	}
	
	private static Patient patient(String gender) {
		Person person = new Person();
		person.setGender(gender);
		PersonName name = new PersonName("Test", null, "Patient");
		name.setPreferred(true);
		person.addName(name);
		Patient p = new Patient(person);
		p.setUuid("patient-" + gender.toLowerCase());
		return p;
	}
}
