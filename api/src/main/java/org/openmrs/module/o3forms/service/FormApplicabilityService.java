/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.o3forms.service;

import java.util.List;

import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.annotation.Authorized;
import org.openmrs.util.PrivilegeConstants;

/**
 * Service for evaluating form applicability rules against a patient/visit context.
 */
public interface FormApplicabilityService {
	
	/**
	 * Filters the supplied list of forms by evaluating each form's "Form Applicability Rule" resource
	 * (if present) against the given patient and visit context using SpEL.
	 * <ul>
	 * <li>Forms with no "Form Applicability Rule" resource are <em>always</em> included.</li>
	 * <li>Forms whose rule evaluates to {@code true} are included.</li>
	 * <li>Forms whose rule evaluates to {@code false} are excluded.</li>
	 * <li>If {@code patient} is {@code null}, the patient variable is not bound; rules that reference
	 * {@code patient} will receive {@code null}.</li>
	 * <li>If {@code visit} is {@code null}, the visit variable is not bound; rules that reference
	 * {@code visit} will receive {@code null}.</li>
	 * </ul>
	 *
	 * @param forms the list of forms to evaluate (must not be {@code null})
	 * @param patient the patient context, or {@code null}
	 * @param visit the visit context, or {@code null}
	 * @return a new list containing only the applicable forms (never {@code null})
	 */
	@Authorized(PrivilegeConstants.GET_FORMS)
	List<Form> getApplicableForms(List<Form> forms, Patient patient, Visit visit);
}
