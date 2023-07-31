package org.openmrs.module.o3.api.exceptions;

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
