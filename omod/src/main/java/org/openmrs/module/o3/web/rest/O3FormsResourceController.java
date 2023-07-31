package org.openmrs.module.o3.web.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmrs.api.context.Context;
import org.openmrs.module.o3.api.exceptions.FormResourcesNotFoundException;
import org.openmrs.module.o3.api.exceptions.FormSchemaNotFoundException;
import org.openmrs.module.o3.api.O3FormsService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.api.RestService;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.openmrs.module.o3.O3Constants.DEFAULT_FORMAT;
import static org.openmrs.module.o3.O3WebConstants.REST_NAMESPACE;

@Controller
@RequestMapping("/rest/" + RestConstants.VERSION_1 + REST_NAMESPACE + "/forms/{formNameOrUuid}")
public class O3FormsResourceController extends BaseRestController {
	
	static {
		System.err.println("O3FormsResourceController loaded");
	}
	
	@GetMapping
	@ResponseBody
	public SimpleObject getO3Form(@PathVariable("formNameOrUuid") String formNameOrUuid,
			@RequestParam(defaultValue = "true") String includeConceptReferences, HttpServletRequest request,
			HttpServletResponse response) {
		// no cache actually means must revalidate; intended to trigger 304s and If-None-Matched when requesting forms
		response.setHeader("Cache-Control", "no-cache; private");
		
		if (formNameOrUuid != null && !formNameOrUuid.trim().isEmpty()) {
			O3FormsService o3FormsService = Context.getService(O3FormsService.class);
			SimpleObject formSchema = new SimpleObject(0);
			try {
				formSchema = o3FormsService.compileFormSchema(formNameOrUuid);
			}
			catch (FormSchemaNotFoundException e) {
				throw new ObjectNotFoundException("Form " + formNameOrUuid + " does not exist");
			}
			catch (FormResourcesNotFoundException e) {
				throw new ObjectNotFoundException("Form " + formNameOrUuid + " does not have any resources");
			}
			
			try {
				formSchema.put("translations", o3FormsService.getTranslations(formNameOrUuid));
			}
			catch (FormSchemaNotFoundException | FormResourcesNotFoundException ignored) {
				// logging is handled in the service; forms without translations should largely work
			}
			
			if (Boolean.parseBoolean(includeConceptReferences)) {
				Representation representation = new CustomRepresentation(DEFAULT_FORMAT);
				// get the "v" param for the representations
				String format = request.getParameter(RestConstants.REQUEST_PROPERTY_FOR_REPRESENTATION);
				if (format != null && !format.isEmpty() && !DEFAULT_FORMAT.equals(format)) {
					representation = Context.getService(RestService.class).getRepresentation(format);
				}
				
				try {
					formSchema.put("conceptReferences", o3FormsService.getConceptReferences(formSchema, representation));
				}
				catch (FormSchemaNotFoundException | FormResourcesNotFoundException ignored) {
					// logging is handled in the service; forms without translations should largely work
				}
			}
			
			return formSchema;
		}
		
		return new SimpleObject(0);
	}
	
	@Override
	public String getNamespace() {
		return RestConstants.VERSION_1 + REST_NAMESPACE + "/forms";
	}
}
