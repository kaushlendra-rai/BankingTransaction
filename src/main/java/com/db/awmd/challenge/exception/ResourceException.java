package com.db.awmd.challenge.exception;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@JsonIgnoreProperties(value = { "cause", "stackTrace", "suppressed" })
public class ResourceException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private HttpStatus httpStatus;
	
	// Application error code
	private int errorCode;
	
	/**
	 * While creating the ResourceException the application error code and httpStatus MUST be specified as this would be returned to the API
	 * consumer in case of any error. 
	 * @param errorMessage Localized error message indicating what went wrong.
	 * @param throwable This throwable instance is maintained only for future reference by application and would never be passed onto the consumer.
	 * @param httpStatus The corresponding HTTP status of the request
	 * @param errorCode The application error code indicating the error encountered.
	 */
	public ResourceException(String errorMessage, Throwable throwable, HttpStatus httpStatus, int errorCode) {
		super(errorMessage, throwable);
		this.httpStatus = httpStatus;
		this.errorCode = errorCode;
	}
	
	/**
	 * While creating the ResourceException the application error code and httpStatus MUST be specified as this would be returned to the API
	 * consumer in case of any error. 
	 * @param errorMessage Localized error message indicating what went wrong.
	 * @param httpStatus The corresponding HTTP status of the request
	 * @param errorCode The application error code indicating the error encountered.
	 */
	public ResourceException(String errorMessage, HttpStatus httpStatus, int errorCode) {
		super(errorMessage);
		this.httpStatus = httpStatus;
		this.errorCode = errorCode;
	}
}