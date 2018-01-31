package org.openbase.bco.registry.lib.provider;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.List;

public interface DeviceClassCollectionProvider {

    public List<DeviceClass> getDeviceClasses() throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsDeviceClassById(final String unitConfigId) throws CouldNotPerformException;

    @RPCMethod
    public DeviceClass getDeviceClassById(final String unitConfigId) throws CouldNotPerformException;
}
