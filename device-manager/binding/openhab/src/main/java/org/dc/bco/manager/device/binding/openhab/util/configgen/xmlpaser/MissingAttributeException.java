/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser;

import nu.xom.Element;

/**
 *
 * @author divine
 */
public class MissingAttributeException extends XMLParsingException {

	public MissingAttributeException(String attributeName, Element sourceElement, Exception e) {
		super("Missing attribute["+attributeName+"] for element["+sourceElement.getQualifiedName()+"].", e);
	}

	public MissingAttributeException(String attributeName, Element sourceElement) {
		super("Missing attribute["+attributeName+"] for element["+sourceElement.getQualifiedName()+"].");
	}
}
