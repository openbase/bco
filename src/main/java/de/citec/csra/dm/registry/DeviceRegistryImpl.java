/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.registry;

import de.citec.jps.core.JPService;
import de.citec.jul.processing.ProtoBufFileProcessor;
import de.citec.jul.rsb.RSBCommunicationService;
import de.citec.jul.rsb.jp.JPScope;
import rsb.RSBException;
import rsb.patterns.LocalServer;
import rst.homeautomation.registry.DeviceRegistryType;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryImpl extends RSBCommunicationService<DeviceRegistryType.DeviceRegistry, DeviceRegistryType.DeviceRegistry.Builder> {

    private ProtoBufFileProcessor<DeviceRegistryType.DeviceRegistry> protoBufFileProcessor;
    
    public DeviceRegistryImpl() {
        super(JPService.getAttribute(JPScope.class).getValue(), loadData());
    }

    @Override
    public void registerMethods(LocalServer server) throws RSBException {
        
    }
    
    private static DeviceRegistryType.DeviceRegistry.Builder loadData() {
        return DeviceRegistryType.DeviceRegistry.newBuilder();
    }
}
