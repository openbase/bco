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
public class NotOneNodeException extends XMLParsingException {
	public NotOneNodeException(String nodeName, Nodes nodes, Node parent, Exception e) {
		super("Found "+nodes.size()+" instead of one node["+nodeName+"] in Element["+parent.getBaseURI()+"].", parent.getBaseURI(), e);
	}

	public NotOneNodeException(String nodeName, Nodes nodes, Node parent) {
		super("Found "+nodes.size()+" instead of one node["+nodeName+"] in Element["+parent.getBaseURI()+"].");
	}
}
