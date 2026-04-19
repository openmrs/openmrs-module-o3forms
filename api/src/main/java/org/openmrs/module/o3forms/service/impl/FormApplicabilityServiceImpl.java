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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openmrs.Form;
import org.openmrs.FormResource;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.o3forms.service.FormApplicabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link FormApplicabilityService}.
 * <p>
 * For each form the service looks for a {@link FormResource} whose name is exactly
 * {@value #APPLICABILITY_RESOURCE_NAME}. If found, it evaluates the resource's value as a Spring
 * Expression Language (SpEL) expression. The expression is evaluated with two variables available
 * in scope:
 * <ul>
 * <li>{@code #patient} – the {@link Patient} object (may be {@code null})</li>
 * <li>{@code #visit} – the {@link Visit} object (may be {@code null})</li>
 * </ul>
 * <p>
 * Only expressions that evaluate to Boolean {@code true} (or the string {@code "true"},
 * case-insensitive) cause the form to be included. Any exception during evaluation is logged and
 * the form is <em>excluded</em> defensively.
 */
@Service("o3forms.FormApplicabilityService")
@Transactional(readOnly = true)
public class FormApplicabilityServiceImpl implements FormApplicabilityService {
	
	private static final Logger log = LoggerFactory.getLogger(FormApplicabilityServiceImpl.class);
	
	/** The exact name of the FormResource that holds the SpEL applicability expression. */
	public static final String APPLICABILITY_RESOURCE_NAME = "Form Applicability Rule";
	
	/**
	 * Thread-safe: SpelExpressionParser is stateless and can be shared.
	 */
	private final ExpressionParser expressionParser = new SpelExpressionParser();
	
	@Override
	public List<Form> getApplicableForms(List<Form> forms, Patient patient, Visit visit) {
		if (forms == null) {
			throw new IllegalArgumentException("forms list must not be null");
		}
		
		FormService formService = Context.getFormService();
		List<Form> applicable = new ArrayList<>(forms.size());
		
		for (Form form : forms) {
			FormResource applicabilityResource = getApplicabilityResource(formService, form);
			
			if (applicabilityResource == null) {
				// AC-7: no rule → always include
				applicable.add(form);
				continue;
			}
			
			String expression = getResourceValueAsString(applicabilityResource);
			if (expression == null || expression.trim().isEmpty()) {
				// Treat a blank rule the same as no rule
				applicable.add(form);
				continue;
			}
			
			if (evaluateExpression(expression, patient, visit, form)) {
				applicable.add(form);
			}
		}
		
		return applicable;
	}
	
	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------
	
	/**
	 * Returns the first {@link FormResource} for {@code form} with name
	 * {@link #APPLICABILITY_RESOURCE_NAME}, or {@code null} if none exists.
	 */
	private FormResource getApplicabilityResource(FormService formService, Form form) {
		Collection<FormResource> resources = formService.getFormResourcesForForm(form);
		if (resources == null) {
			return null;
		}
		for (FormResource resource : resources) {
			if (APPLICABILITY_RESOURCE_NAME.equals(resource.getName())) {
				return resource;
			}
		}
		return null;
	}
	
	/**
	 * Extracts the string value stored in a {@link FormResource}. FormResource stores its value as a
	 * serialised string via the {@code valueReference} field.
	 */
	private String getResourceValueAsString(FormResource resource) {
		try {
			Object value = resource.getValue();
			if (value != null) {
				return value.toString();
			}
		}
		catch (Exception e) {
			log.warn("Could not deserialize value for FormResource '{}', falling back to raw reference.", resource.getName(),
			    e);
		}
		// Fall back to the raw stored string
		return resource.getValueReference();
	}
	
	/**
	 * Evaluates a SpEL {@code expression} string against the given context variables.
	 *
	 * @return {@code true} if the expression evaluates to Boolean {@code true} or the string
	 *         {@code "true"} (case-insensitive); {@code false} otherwise.
	 */
	private boolean evaluateExpression(String expression, Patient patient, Visit visit, Form form) {
		try {
			StandardEvaluationContext ctx = new StandardEvaluationContext();
			ctx.setVariable("patient", patient);
			ctx.setVariable("visit", visit);
			
			Expression compiledExpression = expressionParser.parseExpression(expression);
			Object result = compiledExpression.getValue(ctx);
			
			if (result instanceof Boolean) {
				return (Boolean) result;
			}
			if (result instanceof String) {
				return Boolean.parseBoolean((String) result);
			}
			
			log.warn("Form applicability rule for form '{}' (uuid={}) returned a non-boolean value: {}. "
			        + "Form will be excluded.",
			    form.getName(), form.getUuid(), result);
			return false;
			
		}
		catch (Exception e) {
			log.error(
			    "Error evaluating applicability rule for form '{}' (uuid={}). Expression: [{}]. " + "Form will be excluded.",
			    form.getName(), form.getUuid(), expression, e);
			return false;
		}
	}
}
