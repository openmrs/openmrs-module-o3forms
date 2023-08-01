/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.o3forms.api.exceptions;

import org.openmrs.api.APIException;

/**
 * Exception when the form schema cannot be found
 */
public class FormSchemaNotFoundException extends APIException {
	
	public FormSchemaNotFoundException() {
		super();
	}
	
	public FormSchemaNotFoundException(String message) {
		super(message);
	}
	
	public FormSchemaNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public FormSchemaNotFoundException(Throwable cause) {
		super(cause);
	}
	
	public FormSchemaNotFoundException(String messageKey, Object[] parameters) {
		super(messageKey, parameters);
	}
	
	public FormSchemaNotFoundException(String messageKey, Object[] parameters, Throwable cause) {
		super(messageKey, parameters, cause);
	}
}
