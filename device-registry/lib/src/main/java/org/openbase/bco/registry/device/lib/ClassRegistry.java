package org.openbase.bco.registry.device.lib;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.iface.annotations.RPCMethod;
import org.openbase.jul.pattern.provider.DataProvider;
import rst.domotic.registry.ClassRegistryDataType.ClassRegistryData;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

import java.util.List;
import java.util.concurrent.Future;

public interface ClassRegistry extends DataProvider<ClassRegistryData>, Shutdownable {

    // handle device classes

    @RPCMethod
    Future<DeviceClass> registerDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    Future<DeviceClass> updateDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    Future<DeviceClass> removeDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    Boolean containsDeviceClassById(final String deviceClassId) throws CouldNotPerformException;

    @RPCMethod
    Boolean containsDeviceClass(final DeviceClass deviceClass) throws CouldNotPerformException;

    @RPCMethod
    Boolean isDeviceClassRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    Boolean isDeviceClassRegistryConsistent() throws CouldNotPerformException;

    // handle agent classes

    @RPCMethod
    Future<AgentClass> registerAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    @RPCMethod
    Boolean containsAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    @RPCMethod
    Boolean containsAgentClassById(String agentClassId) throws CouldNotPerformException;

    @RPCMethod
    Future<AgentClass> updateAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    @RPCMethod
    Future<AgentClass> removeAgentClass(AgentClass agentClass) throws CouldNotPerformException;

    List<AgentClass> getAgentClasses() throws CouldNotPerformException;

    @RPCMethod
    Boolean isAgentClassRegistryReadOnly() throws CouldNotPerformException;

    @RPCMethod
    AgentClass getAgentClassById(final String agentClassId) throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the agent class registry is consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAgentClassRegistryConsistent() throws CouldNotPerformException;

    // handle app classes

    @RPCMethod
    Future<AppClass> registerAppClass(AppClass appClass) throws CouldNotPerformException;

    @RPCMethod
    Boolean containsAppClass(AppClass appClass) throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    Boolean containsAppClassById(String appClassId) throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    Future<AppClass> updateAppClass(AppClass appClass) throws CouldNotPerformException;

    @RPCMethod
    Future<AppClass> removeAppClass(AppClass appClass) throws CouldNotPerformException;

    @RPCMethod
    AppClass getAppClassById(final String appClassId) throws CouldNotPerformException, InterruptedException;

    List<AppClass> getAppClasses() throws CouldNotPerformException, InterruptedException;

    @RPCMethod
    Boolean isAppClassRegistryReadOnly() throws CouldNotPerformException, InterruptedException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the app class registry is consistent
     *
     * @throws CouldNotPerformException if the check fails
     */
    @RPCMethod
    Boolean isAppClassRegistryConsistent() throws CouldNotPerformException;

}
