package org.openbase.bco.registry.device.lib;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.concurrent.Future;

public interface ClassRegistry {

    // handle device classes

    @RPCMethod
    public Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    public Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    public Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsDeviceClassById(final String deviceClassId) throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    public Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    public Boolean isDeviceClassRegistryConsistent() throws CouldNotPerformException;

}
