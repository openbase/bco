/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.dal;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.service.HardwareManager;
import de.citec.dal.service.DalRegistry;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thuxohl
 */
public class RSBBindingConnection implements RSBBindingInterface {
    
    private static final Logger logger = LoggerFactory.getLogger(RSBBindingConnection.class);
    
    private static RSBBindingConnection instance;
    private final RSBBindingInterface binding;
    
    private final DalRegistry registry;
    private final HardwareManager hardwareManager;

    public RSBBindingConnection(RSBBindingInterface binding) {
        this.binding = binding;
        this.instance = this;
        this.registry = DalRegistry.getInstance();
        this.hardwareManager = HardwareManager.getInstance();
        
        if(binding != null) {
            hardwareManager.activate();
        }
    }

    @Override
    public void internalReceiveUpdate(String itemName, State newState) {
        logger.debug("Incomming Item[" + itemName + "] State[" + newState.toString() + "].");
        hardwareManager.internalReceiveUpdate(itemName, newState);
    }

    @Override
    public Future executeCommand(String itemName, Command command, ExecutionType type) throws RSBBindingException {
        return binding.executeCommand(itemName, command, type);
    }
    
    public static RSBBindingInterface getInstance() {
        while(instance == null) {
           logger.warn("WARN: Binding not ready yet!");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(RSBBindingConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
        return instance;
    }
}
