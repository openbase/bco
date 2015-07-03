/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent;

import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.iface.Activatable;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;

/**
 *
 * @author mpohling
 */
public interface AgentInterface extends Activatable {
    public AgentConfig getConfig() throws NotAvailableException;
}
