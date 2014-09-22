/*
 * Copyright (c) 2014 openHAB.org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    openHAB.org - initial API and implementation and/or initial documentation
 */
package de.citec.dal.exception;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public class RSBBindingException extends Exception {

	public RSBBindingException(String message) {
		super(message);
	}

	public RSBBindingException(String message, Throwable cause) {
		super(message, cause);
	}

	public RSBBindingException(Throwable cause) {
		super(cause);
	}

	public RSBBindingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
