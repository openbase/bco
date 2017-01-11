package org.openbase.bco.manager.device.binding.openhab.util.configgen.xmlpaser;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import nu.xom.ParsingException;

/*
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class XMLParsingException extends ParsingException {


	public XMLParsingException(String message) {
		super(message);
	}
	public XMLParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	public XMLParsingException(String message, String uri, Throwable cause) {
		super(message, uri, cause);
	}

	public XMLParsingException(String message, int lineNumber, int columnNumber) {
		super(message, lineNumber, columnNumber);
	}

	public XMLParsingException(String message, String uri, int lineNumber, int columnNumber) {
		super(message, uri, lineNumber, columnNumber);
	}

	public XMLParsingException(String message, String uri, int lineNumber, int columnNumber, Throwable cause) {
		super(message, uri, lineNumber, columnNumber, cause);
	}

	public XMLParsingException(String message, int lineNumber, int columnNumber, Throwable cause) {
		super(message, lineNumber, columnNumber, cause);
	}
}
