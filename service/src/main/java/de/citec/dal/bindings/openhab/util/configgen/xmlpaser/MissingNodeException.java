/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util.configgen.xmlpaser;

import nu.xom.Node;

/**
 *
 * @author divine
 */
public class MissingNodeException extends XMLParsingException {

	public MissingNodeException(String nodeName, Node parent, Exception e) {
		super("Missing child node["+nodeName+"] for Element["+parent.getBaseURI()+"].", e);
	}

	public MissingNodeException(String nodeName, Node parent) {
		super("Missing child node["+nodeName+"] for Element["+parent.getBaseURI()+"].");
	}
}
