/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.exception.RSBBindingException;
import java.util.concurrent.Future;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 *
 * @author thuxohl
 */
public interface RSBBindingInterface {
    
    
    enum ExecutionType {SYNCHRONOUS, ASYNCHRONOUS};
            
    void internalReceiveUpdate(String itemName, State newState);
    
    Future executeCommand(String itemName, Command command, ExecutionType type) throws RSBBindingException;
}
