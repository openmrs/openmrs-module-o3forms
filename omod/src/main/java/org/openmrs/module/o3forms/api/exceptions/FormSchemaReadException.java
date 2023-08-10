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

public class FormSchemaReadException extends APIException {
	
	public FormSchemaReadException() {
		super();
	}
	
	public FormSchemaReadException(String message) {
		super(message);
	}
	
	public FormSchemaReadException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public FormSchemaReadException(Throwable cause) {
		super(cause);
	}
	
	public FormSchemaReadException(String messageKey, Object[] parameters) {
		super(messageKey, parameters);
	}
	
	public FormSchemaReadException(String messageKey, Object[] parameters, Throwable cause) {
		super(messageKey, parameters, cause);
	}
}
