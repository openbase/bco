/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class MultiException extends Exception {

	private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Object, Exception> exceptionMap = new HashMap<>();
    
    public MultiException(final String message, final Map<Object, Exception> exceptions) {
        super(message);
        exceptionMap.putAll(exceptions);
    }

	public Map<Object, Exception> getExceptionStack() {
		return Collections.unmodifiableMap(exceptionMap);
	}

	public void printExceptionStack() {
		for(Object source : exceptionMap.keySet()) {
			logger.error("Exception from "+source.toString()+":", exceptionMap.get(source));
		}
	}
}
