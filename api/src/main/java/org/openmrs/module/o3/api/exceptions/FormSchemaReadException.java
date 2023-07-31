package org.openmrs.module.o3.api.exceptions;

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
