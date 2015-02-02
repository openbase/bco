/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import de.citec.dal.exception.RSBBindingException;
import de.citec.jul.exception.CouldNotPerformException;
import java.util.concurrent.Future;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;

/**
 *
 * @author thuxohl
 */
public interface OpenhabBindingInterface {
            
    void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException;
    
    Future executeCommand(OpenhabCommand command) throws RSBBindingException;
}
