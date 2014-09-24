/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.exception.RSBBindingException;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 *
 * @author thuxohl
 */
public interface RSBBindingInterface {
    
    void internalReceiveCommand(String itemName, Command command);

    void internalReceiveUpdate(String itemName, State newState);

    void postCommand(String itemName, Command command) throws RSBBindingException;

    void sendCommand(String itemName, Command command) throws RSBBindingException;
}
