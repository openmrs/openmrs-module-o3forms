/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.o3.api;

import java.util.Map;

import org.openmrs.Form;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The main service of this module, which is exposed for other modules. See
 * moduleApplicationContext.xml on how it is wired up.
 */
public interface O3FormsService extends OpenmrsService {
	
	/**
	 * Compiles a form, returning a form with all references replaced by the appropriate object.
	 * <p/>
	 * In O3 forms, a reference is an element like this:
	 * <p/>
	 * <pre>
	 * {
	 *       "reference": {
	 *           "form": &lt;form alias&gt;,
	 *           &lt;additional properties&gt;
	 *       }
	 * }
	 * </pre>
	 * <p/>
	 * E.g.,
	 * <pre>
	 * {
	 *       "reference": {
	 *           "form": "pcr",
	 *           "page": "Pre-Clinical Review",
	 *           "section": "Pre-Clinical Review
	 *       }
	 * }
	 * </pre>
	 * <p/>
	 * References the section labelled "Pre-Clinical Review" in the page labelled "Pre-Clinical Review" in the form with the
	 * alias "pcr". After compilation, this element will be replaced with the contents of the appropriate section.
	 *
	 * @param formNameOrUuid The name or UUID of the form
	 * @return A {@link SimpleObject} that represents the compiled form contents
	 */
	SimpleObject compileFormSchema(String formNameOrUuid);
	
	/**
	 * Compiles a form, returning a form with all references replaced by the appropriate object.
	 * <p/>
	 * In O3 forms, a reference is an element like this:
	 * <p/>
	 * <pre>
	 * {
	 *       "reference": {
	 *           "form": &lt;form alias&gt;,
	 *           &lt;additional properties&gt;
	 *       }
	 * }
	 * </pre>
	 * <p/>
	 * E.g.,
	 * <pre>
	 * {
	 *       "reference": {
	 *           "form": "pcr",
	 *           "page": "Pre-Clinical Review",
	 *           "section": "Pre-Clinical Review
	 *       }
	 * }
	 * </pre>
	 * <p/>
	 * References the section labelled "Pre-Clinical Review" in the page labelled "Pre-Clinical Review" in the form with the
	 * alias "pcr". After compilation, this element will be replaced with the contents of the appropriate section.
	 *
	 * @param form The form object
	 * @return A {@link SimpleObject} that represents the compiled form contents
	 */
	SimpleObject compileFormSchema(Form form);
	
	SimpleObject getConceptReferences(SimpleObject compiledForm);
	
	@Transactional(readOnly = true)
	SimpleObject getConceptReferences(SimpleObject compiledForm, Representation conceptRepresentation);
	
	/**
	 * Loads the translations associated with a particular form. These translations are defined in additional form resources
	 * attached to the form. If referenced forms are included, then the appropriate translations for the referenced forms
	 * (if any) will also be included.
	 * <p/>
	 * For each form, only a single translation file will be created, the first to match the appropriate locale as determined
	 * by {@link org.openmrs.util.LocaleUtility#getLocalesInOrder()}. If multiple forms are referenced, the translations
	 * will be merged into a single JSON object, with keys from the explicitly loaded form overwriting keys from referenced
	 * forms.
	 *
	 * @param formNameOrUuid The name or UUID of the form
	 * @return A map of translation key-translation pairs for the form
	 */
	Map<String, ?> getTranslations(String formNameOrUuid);
	
	/**
	 * Loads the translations associated with a particular form. These translations are defined in additional form resources
	 * attached to the form. If referenced forms are included, then the appropriate translations for the referenced forms
	 * (if any) will also be included.
	 * <p/>
	 * For each form, only a single translation file will be created, the first to match the appropriate locale as determined
	 * by {@link org.openmrs.util.LocaleUtility#getLocalesInOrder()}. If multiple forms are referenced, the translations
	 * will be merged into a single JSON object, with keys from the explicitly loaded form overwriting keys from referenced
	 * forms.
	 *
	 * @param form The form
	 * @return A map of translation key-translation pairs for the form
	 */
	Map<String, ?> getTranslations(Form form);
}
