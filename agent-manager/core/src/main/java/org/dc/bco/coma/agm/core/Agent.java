/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.agm.core;

import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.iface.Activatable;
import de.citec.jul.iface.Identifiable;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author mpohling
 */
public interface Agent extends Activatable, Identifiable<String> {
    public AgentConfig getConfig() throws NotAvailableException;
}
