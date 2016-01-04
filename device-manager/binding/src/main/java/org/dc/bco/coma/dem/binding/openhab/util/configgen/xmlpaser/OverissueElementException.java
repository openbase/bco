/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab.util.configgen.xmlpaser;

import nu.xom.Element;
import nu.xom.Elements;

/**
 *
 * @author divine
 */
public class OverissueElementException extends XMLParsingException {
	public OverissueElementException(String elementName, Elements childElements, Element parent, Exception e) {
		super("Expected one Element["+elementName+"] but found " + childElements.size() + " childs of parent Element["+parent.getLocalName()+"].", e);
	}

	public OverissueElementException(String elementName, Elements childElements, Element parent) {
		super("Expected one Element["+elementName+"] but found " + childElements.size() + " childs of parent Element["+parent.getLocalName()+"].");
	}
}
