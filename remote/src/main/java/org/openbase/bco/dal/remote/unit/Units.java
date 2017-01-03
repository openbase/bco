package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import com.google.protobuf.GeneratedMessage;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.storage.registry.RemoteControllerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rst.domotic.unit.UnitConfigType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class Units {

    private static final Logger LOGGER = LoggerFactory.getLogger(Units.class);

    public static Units instance;

    private static final ReentrantReadWriteLock unitRemoteRegistryLock = new ReentrantReadWriteLock();
    private static final UnitRemoteFactory unitRemoteFactory = UnitRemoteFactoryImpl.getInstance();

    private static RemoteControllerRegistry<String, UnitRemote<? extends GeneratedMessage, UnitConfig>> unitRemoteRegistry;
    private static UnitRegistry unitRegistry;

    static {
        try {
            unitRemoteRegistry = new RemoteControllerRegistry<>();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException(new org.openbase.jul.exception.InstantiationException(Units.class, ex)), LOGGER);
        }
    }

    /**
     * Method returns an instance of the unit registry. If this method is called for the first time, a new registry instance will be created.
     * This method only returns if the unit registry synchronization is finished otherwise this method blocks forever.
     * In case you won't wait for the synchronization, request your own instance by calling: <code>Registries.getUnitRegistry();</code>
     *
     * @return the unit registry instance.
     * @throws InterruptedException is thrown if the current thread was externally interrupted.
     * @throws CouldNotPerformException Is thrown in case an error occurs during registry connection.
     */
    public synchronized static UnitRegistry getUnitRegistry() throws InterruptedException, CouldNotPerformException {
        if (unitRegistry == null) {
            unitRegistry = CachedUnitRegistryRemote.getRegistry();
            CachedUnitRegistryRemote.waitForData();
        }
        return unitRegistry;
    }

    /**
     * Returns the unit remote of the unit identified by the given unit id.
     *
     * @param unitId the unit id to identify the unit.
     * @return a new created or already cached unit remote.
     * @throws NotAvailableException is thrown if the unit remote was not available.
     * @throws InterruptedException is thrown if the current thread was externally interrupted.
     */
    private static UnitRemote getUnitRemote(final String unitId) throws NotAvailableException, InterruptedException {
        final boolean newInstance;
        final UnitRemote unitRemote;
        unitRemoteRegistryLock.writeLock().lock();
        try {
            try {
                if (!unitRemoteRegistry.contains(unitId)) {
                    // create new instance.
                    newInstance = true;
                    unitRemote = unitRemoteFactory.newInitializedInstance(getUnitRegistry().getUnitConfigById(unitId));
                    unitRemoteRegistry.register(unitRemote);

                } else {
                    // return cached instance.
                    newInstance = false;
                    unitRemote = unitRemoteRegistry.get(unitId);
                }
            } finally {
                unitRemoteRegistryLock.writeLock().unlock();
            }

            // The activation is not synchronized by the unitRemoteRegistryLock out of performance reasons. 
            // By this, new unit remotes can be requested independend from the activation state of other units.
            if (newInstance) {
                unitRemote.activate();
            }
            return unitRemote;
        } catch (CouldNotPerformException | NullPointerException ex) {
            throw new NotAvailableException("UnitRemote[" + unitId + "]", ex);
        }
    }

    /**
     * Returns the unit remote of the unit identified by the given unit config.
     *
     * @param unitConfig the unit config to identify the unit.
     * @return a new created or already cached unit remote.
     * @throws NotAvailableException is thrown if the unit remote was not available.
     * @throws InterruptedException is thrown if the current thread was externally interrupted.
     */
    private static UnitRemote getUnitRemote(final UnitConfig unitConfig) throws NotAvailableException, InterruptedException {
        final boolean newInstance;
        final UnitRemote unitRemote;
        unitRemoteRegistryLock.writeLock().lock();
        try {
            try {
                if (!unitRemoteRegistry.contains(unitConfig.getId())) {
                    // create new instance.
                    newInstance = true;
                    unitRemote = unitRemoteFactory.newInitializedInstance(unitConfig);
                    unitRemoteRegistry.register(unitRemote);

                } else {
                    // return cached instance.
                    newInstance = false;
                    unitRemote = unitRemoteRegistry.get(unitConfig.getId());
                }
            } finally {
                unitRemoteRegistryLock.writeLock().unlock();
            }

            // The activation is not synchronized by the unitRemoteRegistryLock out of performance reasons. 
            // By this, new unit remotes can be requested independend from the activation state of other units.
            if (newInstance) {
                unitRemote.activate();
            }
            return unitRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitRemote[" + unitConfig.getId() + "]", ex);
        }
    }

    /**
     * Method waits for unit data if the waitForData flag is set.
     *
     * @param unitRemote the remote to wait for data.
     * @param waitForData the flag to decide if the current thread should be wait for the uni data.
     * @return the given unit remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the wait phase.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    private static UnitRemote waitForData(final UnitRemote unitRemote, final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            unitRemote.waitForData();
        }
        return unitRemote;
    }

    /**
     * Method establishes a connection to the unit referred by the given unit identifier.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.remote.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param unitId the unit identifier.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     * @throws NotAvailableException is thrown in case the unit is not available.
     * @throws InterruptedException is thrown in case the thread is externally interrupted
     */
    public static UnitRemote getUnit(final String unitId, final boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (unitId == null) {
                assert false;
                throw new NotAvailableException("UnitId");
            }
            return waitForData(getUnitRemote(unitId), waitForData);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + unitId + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit identifier.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.remote.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param unitId the unit identifier.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     * @throws NotAvailableException is thrown in case the unit is not available.
     * @throws InterruptedException is thrown in case the thread is externally interrupted
     */
    public static UnitRemote getUnit(final UnitConfig unitConfig, final boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (unitConfig == null) {
                assert false;
                throw new NotAvailableException("UnitConfig");
            }
            return waitForData(getUnitRemote(unitConfig), waitForData);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + unitConfig.getId() + "|" + unitConfig.getLabel() + "]", ex);
        }
    }

    /**
     *
     * This method is a wrapper for {@link #getUnit(rst.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)} and casts the result to the given {@code unitRemoteClass}.
     *
     * @param <UR> the unit remote class type.
     * @param unitConfig Checkout wrapped method doc {@link #getUnit(rst.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     * @param waitForData Checkout wrapped method doc {@link #getUnit(rst.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     * @param unitRemoteClass the unit remote class.
     * @return an instance of the given remote class.
     * @throws NotAvailableException Is thrown if the remote instance is not compatible with the given class. See {{@link #getUnit(rst.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)} for further cases.
     * @throws InterruptedException Checkout wrapped method doc {@link #getUnit(rst.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     *
     * @see #getUnit(rst.domotic.unit.UnitConfigType.UnitConfig, boolean)
     */
    public static <UR extends UnitRemote> UR getUnit(final UnitConfig unitConfig, boolean waitForData, final Class<UR> unitRemoteClass) throws NotAvailableException, InterruptedException {
        try {
            return (UR) getUnit(unitConfig, waitForData);
        } catch (ClassCastException ex) {
            throw new NotAvailableException("Unit[" + unitConfig.getId() + "]", new InvalidStateException("Requested Unit[" + unitConfig.getLabel() + "] of UnitType[" + unitConfig.getType() + "] is not compatible with defined UnitRemoteClass[" + unitRemoteClass + "]!", ex));
        }
    }

    /**
     *
     * This method is a wrapper for {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)} and casts the result to the given {@code unitRemoteClass}.
     *
     * @param <UR> the unit remote class type.
     * @param unitId Checkout wrapped method doc {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)}
     * @param waitForData Checkout wrapped method doc {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)}
     * @param unitRemoteClass the unit remote class.
     * @return an instance of the given remote class.
     * @throws NotAvailableException Is thrown if the remote instance is not compatible with the given class. See {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)} for further cases.
     * @throws InterruptedException Checkout wrapped method doc {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)}
     *
     * @see #getUnit(java.lang.String, boolean)
     */
    public static <UR extends UnitRemote> UR getUnit(final String unitId, boolean waitForData, final Class<UR> unitRemoteClass) throws NotAvailableException, InterruptedException {
        try {
            return (UR) getUnit(unitId, waitForData);
        } catch (ClassCastException ex) {
            throw new NotAvailableException("Unit[" + unitId + "]", new InvalidStateException("Requested Unit[" + unitId + "] is not compatible with defined UnitRemoteClass[" + unitRemoteClass + "]!", ex));
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit label.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.remote.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param label the label to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    public static UnitRemote getUnitByLabel(final String label, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (label == null) {
                assert false;
                throw new NotAvailableException("UnitName");
            }
            final List<UnitConfigType.UnitConfig> unitConfigList = getUnitRegistry().getUnitConfigsByLabel(label);

            if (unitConfigList.isEmpty()) {
                throw new NotAvailableException("No configuration found in registry!");
            } else if (unitConfigList.size() > 1) {
                throw new InvalidStateException("Unit is not unique! Please specify the unit location in order to unique resolve the unit configuration.");
            }

            return getUnit(unitConfigList.get(0), waitForData);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + label + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit scope.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.remote.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param scope the scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    public static UnitRemote getUnitByScope(final ScopeType.Scope scope, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (scope == null) {
                assert false;
                throw new NotAvailableException("UnitScope");
            }
            return getUnit(getUnitRegistry().getUnitConfigByScope(scope), waitForData);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + ScopeGenerator.generateStringRep(scope) + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit scope.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.remote.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param scope the scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    public static UnitRemote getUnitByScope(final Scope scope, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (scope == null) {
                assert false;
                throw new NotAvailableException("UnitScope");
            }
            return getUnitByScope(ScopeTransformer.transform(scope), waitForData);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + scope + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit scope.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.remote.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param scope the scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    public static UnitRemote getUnitByScope(final String scope, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (scope == null) {
                assert false;
                throw new NotAvailableException("UnitScope");
            }
            return getUnitByScope(ScopeGenerator.generateScope(scope), waitForData);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + scope + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit label and location scope.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.remote.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param label the label to identify the unit.
     * @param locationScope the location scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    public UnitRemote getUnitByLabelAndLocationScope(final String label, final String locationScope, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (label == null) {
                assert false;
                throw new NotAvailableException("UnitLabel");
            }
            if (locationScope == null) {
                assert false;
                throw new NotAvailableException("UnitLocationScope");
            }

            for (UnitConfig unitConfig : getUnitRegistry().getUnitConfigsByLabel(label)) {
                if (ScopeGenerator.generateStringRep(getUnitRegistry().getUnitConfigById(unitConfig.getPlacementConfig().getLocationId()).getScope()).equals(locationScope)) {
                    return getUnit(unitConfig, waitForData);
                }
            }
            throw new InvalidStateException("No unit with Label[" + label + "] found at Location[" + locationScope + "]");

        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + label + "]", ex);
        }
    }

//    public enum UnitClass {
//        LIGHT(LightRemote.class);
//
//        final Class unitRemoteClass;
//
//        private UnitClass(final Class unitRemoteClass) {
//            this.unitRemoteClass = unitRemoteClass;
//        }
//
//        public Class getUnitClass() {
//            return unitRemoteClass;
//        }
//    }
//    
//    public static <UR extends UnitRemote> UR getUnit(final String unitId, boolean waitForData, final UnitClass unitClass) throws NotAvailableException, InterruptedException {
//        try {
//            return (UR) getUnit(unitId, waitForData, unitClass.getUnitClass());
//        } catch (ClassCastException ex) {
//            throw new NotAvailableException("Unit[" + unitId + "]", new InvalidStateException("Requested Unit[" + unitId + "] is not compatible with defined UnitRemoteClass[" + unitRemoteClass + "]!", ex));
//        }
//    }

    public static LightRemote getLightUnit(final UnitConfig unitConfig, boolean waitForData) throws NotAvailableException, InterruptedException {
        return getUnit(unitConfig, waitForData, LightRemote.class);
    }
}
