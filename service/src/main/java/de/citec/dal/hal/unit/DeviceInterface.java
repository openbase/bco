/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.data.Location;
import de.citec.dal.hal.service.ServiceFactory;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 */
public interface DeviceInterface {

    public String getId();

    public Location getLocation();

    public String getHardware_id();

    public String getInstance_id();
    
    public ServiceFactory getDefaultServiceFactory();
}
