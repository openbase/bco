/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author mpohling
 */
public class MultiException extends Exception {

    private final List<Exception> exceptionStack = new ArrayList<>();
    
    public MultiException(String message, Collection<Exception> exceptions) {
        super(message);
        exceptionStack.addAll(exceptions);
    }

    public List<Exception> getExceptionStack() {
        return exceptionStack;
    }
}
