/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

import org.dc.bco.dal.lib.layer.service.provider.TemperatureProvider;
import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public interface TemperatureControllerInterface extends TemperatureProvider, TargetTemperatureService {
    
}
