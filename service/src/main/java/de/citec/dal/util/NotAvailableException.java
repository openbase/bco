/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

/**
 *
 * @author mpohling
 */
public class NotAvailableException extends DALException {

    public NotAvailableException(String label) {
        super(label+ " is not available!");
    }

    public NotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAvailableException(Throwable cause) {
        super(cause);
    }

    public NotAvailableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
 
    
}
