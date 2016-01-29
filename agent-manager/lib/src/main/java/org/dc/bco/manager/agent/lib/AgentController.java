/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.lib;

import org.dc.jul.exception.InitializationException;
import org.dc.jul.iface.Activatable;
import org.dc.jul.iface.Configurable;
import org.dc.jul.iface.Enableable;
import org.dc.jul.iface.Identifiable;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine
 * Threepwood</a>
 */
public interface AgentController extends Identifiable<String>, Configurable<String, AgentConfig>, Activatable, Enableable, Agent {

    public void init(final AgentConfig config) throws InitializationException, InterruptedException;

}
