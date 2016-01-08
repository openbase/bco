/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser;

import nu.xom.ParsingException;

/*
 * @author mpohling
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
