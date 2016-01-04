/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab.util.configgen.xmlpaser;

import nu.xom.Node;
import nu.xom.Nodes;

/**
 *
 * @author divine
 */
public class OverissueNodeException extends XMLParsingException {

	public OverissueNodeException(String nodeName, Nodes childNodes, Node parent, Exception e) {
		super("Expected one Node[" + nodeName + "] but found " + childNodes.size() + " childs of parent Element[" + parent.getBaseURI() + "].", parent.getBaseURI(), e);
	}

	public OverissueNodeException(String nodeName, Nodes childElements, Node parent) {
		super("Expected one Node[" + nodeName + "] but found " + childElements.size() + " childs of parent Element[" + parent.getBaseURI() + "].");
	}
}