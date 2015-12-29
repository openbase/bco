/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.agm.lib.jp;

import org.dc.jps.preset.AbstractJPString;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class JPAgentId extends AbstractJPString {

    public final static String[] COMMAND_IDENTIFIERS = {"agent-id"};
    
    public JPAgentId() {
        super(COMMAND_IDENTIFIERS);
    }
    
    @Override
    protected String getPropertyDefaultValue() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Agent id to resolv the agent configuration.";
    }
    
}
