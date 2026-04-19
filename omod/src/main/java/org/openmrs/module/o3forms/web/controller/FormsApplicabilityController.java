/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.o3forms.web.controller;

import java.util.ArrayList;
import java.util.List;

import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.FormService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.o3forms.service.FormApplicabilityService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST endpoint: GET /ws/rest/v1/o3forms/forms Returns all non-retired forms, optionally filtered
 * by applicability rules evaluated against a patient and/or visit context using Spring Expression
 * Language (SpEL). Query parameters: patient (optional) – UUID of the patient to use as evaluation
 * context visit (optional) – UUID of the visit to use as evaluation context
 */
@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/o3forms")
public class FormsApplicabilityController {
	
	private final FormApplicabilityService formApplicabilityService;
	
	@Autowired
	public FormsApplicabilityController(FormApplicabilityService formApplicabilityService) {
		this.formApplicabilityService = formApplicabilityService;
	}
	
	/**
	 * GET /ws/rest/v1/o3forms/forms
	 *
	 * @param patientUuid optional UUID of the patient
	 * @param visitUuid optional UUID of the visit
	 * @return list of applicable form representations
	 */
	@RequestMapping(value = "/forms", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SimpleObject> getApplicableForms(@RequestParam(value = "patient", required = false) String patientUuid,
	        @RequestParam(value = "visit", required = false) String visitUuid) {
		
		// Resolve patient (null if UUID not provided or not found)
		Patient patient = null;
		if (patientUuid != null && !patientUuid.trim().isEmpty()) {
			PatientService patientService = Context.getPatientService();
			patient = patientService.getPatientByUuid(patientUuid);
			// Per AC-3: if UUID supplied but patient not found, treat as no patient (return all)
		}
		
		// Resolve visit (null if UUID not provided or not found)
		Visit visit = null;
		if (visitUuid != null && !visitUuid.trim().isEmpty()) {
			VisitService visitService = Context.getVisitService();
			visit = visitService.getVisitByUuid(visitUuid);
		}
		
		// Fetch all non-retired forms
		FormService formService = Context.getFormService();
		List<Form> allForms = formService.getAllForms(false);
		
		// Filter by applicability rules and build response
		List<Form> applicableForms = formApplicabilityService.getApplicableForms(allForms, patient, visit);
		
		return buildResponse(applicableForms);
	}
	
	/**
	 * Maps a list of Forms to a list of SimpleObjects suitable for JSON serialisation.
	 */
	private List<SimpleObject> buildResponse(List<Form> forms) {
		List<SimpleObject> result = new ArrayList<>(forms.size());
		for (Form form : forms) {
			SimpleObject obj = new SimpleObject();
			obj.put("uuid", form.getUuid());
			obj.put("name", form.getName());
			obj.put("display", form.getName());
			obj.put("version", form.getVersion());
			obj.put("description", form.getDescription());
			obj.put("published", form.getPublished());
			obj.put("retired", form.getRetired());
			result.add(obj);
		}
		return result;
	}
}
