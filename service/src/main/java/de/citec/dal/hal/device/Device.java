/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device;

import de.citec.dal.data.Location;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.iface.Activatable;
import de.citec.jul.iface.Identifiable;
import de.citec.jul.extension.rsb.scope.ScopeProvider;
import de.citec.jul.iface.provider.LabelProvider;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author Divine Threepwood
 */
public interface Device extends ScopeProvider, LabelProvider, Identifiable<String>, Activatable {

    public DeviceConfig getConfig();

    public Location getLocation();
    
    public ServiceFactory getServiceFactory() throws NotAvailableException;
}
