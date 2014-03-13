/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.unibi.csra.dm.exception;

/**
 *
 * @author mpohling
 */
public class NotAvailableException extends Exception {

	public NotAvailableException(String context) {
		super("["+context+"] is not available!");
	}

	public NotAvailableException(String context, Throwable cause) {
		super("["+context+"] is not available!", cause);
	}

	public NotAvailableException(Throwable cause) {
		super(cause);
	}

	public NotAvailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
	
}
