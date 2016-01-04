/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dc.bco.coma.dem.binding.openhab.util.configgen.xmlpaser;

import nu.xom.Element;


/**
 *
 * @author divine
 */
public class MissingElementException extends XMLParsingException {
	

	public MissingElementException(String elementName, Element parent, Exception e) {
		super("Missing child element["+elementName+"] for Element["+parent.getLocalName()+"].", e);
	}

	public MissingElementException(String elementName, Element parent) {
		super("Missing child element["+elementName+"] for Element["+parent.getLocalName()+"].");
	}
}
