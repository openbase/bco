/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.provider.TemperatureProvider;
import de.citec.dal.hal.service.TargetTemperatureService;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public interface TemperatureControllerInterface extends TemperatureProvider, TargetTemperatureService {
    
}
