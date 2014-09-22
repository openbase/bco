/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.dal.exception;

/**
 *
 * @author mpohling
 */
public class TypeNotSupportedException extends RSBBindingException {

	public TypeNotSupportedException(String message) {
		super(message);
	}

	public TypeNotSupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public TypeNotSupportedException(Throwable cause) {
		super(cause);
	}
	public TypeNotSupportedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
	public TypeNotSupportedException(Object target, Class destination) {
		super("\"Type tranformation not supported! Can not convert "+target+" to "+destination+"!");
	}
		
}
