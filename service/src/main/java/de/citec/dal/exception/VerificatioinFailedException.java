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
public class VerificatioinFailedException extends RSBBindingException {

    public VerificatioinFailedException(String message) {
        super(message);
    }

    public VerificatioinFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public VerificatioinFailedException(Throwable cause) {
        super(cause);
    }

    public VerificatioinFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
