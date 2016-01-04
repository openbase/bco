/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab;

import org.dc.jul.exception.CouldNotPerformException;
import java.util.concurrent.Future;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;

/**
 *
 * @author thuxohl
 */
public interface OpenHABBindingInterface {
            
    void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException;
    
    Future executeCommand(OpenhabCommand command) throws CouldNotPerformException;
}
