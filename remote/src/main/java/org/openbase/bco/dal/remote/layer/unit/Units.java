package org.openbase.bco.dal.remote.layer.unit;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.layer.unit.agent.AgentRemote;
import org.openbase.bco.dal.remote.layer.unit.app.AppRemote;
import org.openbase.bco.dal.remote.layer.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.layer.unit.device.DeviceRemote;
import org.openbase.bco.dal.remote.layer.unit.location.LocationRemote;
import org.openbase.bco.dal.remote.layer.unit.scene.SceneRemote;
import org.openbase.bco.dal.remote.layer.unit.unitgroup.UnitGroupRemote;
import org.openbase.bco.dal.remote.layer.unit.user.UserRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessageMap;
import org.openbase.jul.extension.protobuf.ProtobufListDiff;
import org.openbase.bco.registry.lib.generator.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.RemoteControllerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.rct.Transform;
import rsb.Scope;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.unit.UnitConfigType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.communication.ScopeType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * <p>
 * This is the global unit remote pool of bco. It allows the access and control
 * of all bco units without taking care of the internal unit remote instances.
 * All requested instances are already initialized and activated. Even the
 * shutdown of all instances is managed by this pool.
 */
public class Units {

    /**
     * BASE Unit remote-class-constants which can be used to request unit
     * remotes.
     */
    public static final Class<? extends AgentRemote> BASE_UNIT_AGENT = AgentRemote.class;
    public static final Class<? extends AppRemote> BASE_UNIT_APP = AppRemote.class;
    public static final Class<? extends SceneRemote> BASE_UNIT_SCENE = SceneRemote.class;
    public static final Class<? extends UnitGroupRemote> BASE_UNIT_UNITGROUP = UnitGroupRemote.class;
    public static final Class<? extends UserRemote> BASE_UNIT_USER = UserRemote.class;
    public static final Class<? extends DeviceRemote> BASE_UNIT_DEVICE = DeviceRemote.class;
    public static final Class<? extends LocationRemote> BASE_UNIT_LOCATION = LocationRemote.class;
    public static final Class<? extends ConnectionRemote> BASE_UNIT_CONNECTION = ConnectionRemote.class;
    public static final Class<? extends ConnectionRemote> BASE_UNIT_CONNECTION_DOOR = ConnectionRemote.class;
    public static final Class<? extends ConnectionRemote> BASE_UNIT_CONNECTION_WINDOW = ConnectionRemote.class;
    public static final Class<? extends ConnectionRemote> BASE_UNIT_CONNECTION_PASSAGE = ConnectionRemote.class;
    // comfort
    public static final Class<? extends AgentRemote> UNIT_BASE_AGENT = BASE_UNIT_AGENT;
    public static final Class<? extends AppRemote> UNIT_BASE_APP = BASE_UNIT_APP;
    public static final Class<? extends SceneRemote> UNIT_BASE_SCENE = BASE_UNIT_SCENE;
    public static final Class<? extends UnitGroupRemote> UNIT_UNITGROUP = BASE_UNIT_UNITGROUP;
    public static final Class<? extends UserRemote> UNIT_BASE_USER = BASE_UNIT_USER;
    public static final Class<? extends DeviceRemote> UNIT_BASE_DEVICE = BASE_UNIT_DEVICE;
    public static final Class<? extends LocationRemote> UNIT_BASE_LOCATION = BASE_UNIT_LOCATION;
    public static final Class<? extends ConnectionRemote> UNIT_BASE_CONNECTION = BASE_UNIT_CONNECTION;
    public static final Class<? extends ConnectionRemote> UNIT_BASE_CONNECTION_DOOR = BASE_UNIT_CONNECTION;
    public static final Class<? extends ConnectionRemote> UNIT_BASE_CONNECTION_WINDOW = BASE_UNIT_CONNECTION;
    public static final Class<? extends ConnectionRemote> UNIT_BASE_CONNECTION_PASSAGE = BASE_UNIT_CONNECTION;
    // simple
    public static final Class<? extends AgentRemote> AGENT = BASE_UNIT_AGENT;
    public static final Class<? extends AppRemote> APP = BASE_UNIT_APP;
    public static final Class<? extends SceneRemote> SCENE = BASE_UNIT_SCENE;
    public static final Class<? extends UnitGroupRemote> UNITGROUP = BASE_UNIT_UNITGROUP;
    public static final Class<? extends UserRemote> USER = BASE_UNIT_USER;
    public static final Class<? extends DeviceRemote> DEVICE = BASE_UNIT_DEVICE;
    public static final Class<? extends LocationRemote> LOCATION = BASE_UNIT_LOCATION;
    public static final Class<? extends ConnectionRemote> DOOR = BASE_UNIT_CONNECTION;
    public static final Class<? extends ConnectionRemote> WINDOW = BASE_UNIT_CONNECTION;
    public static final Class<? extends ConnectionRemote> PASSAGE = BASE_UNIT_CONNECTION;
    public static final Class<? extends ConnectionRemote> CONNECTION = BASE_UNIT_CONNECTION;
    /**
     * DAL Unit remote-class-constants which can be used to request unit
     * remotes.
     */
    public static final Class<? extends LightRemote> DAL_UNIT_LIGHT = LightRemote.class;
    public static final Class<? extends LightSensorRemote> DAL_UNIT_LIGHT_SENSOR = LightSensorRemote.class;
    public static final Class<? extends ColorableLightRemote> DAL_UNIT_COLORABLE_LIGHT = ColorableLightRemote.class;
    public static final Class<? extends DimmableLightRemote> DAL_UNIT_DIMMABLE_LIGHT = DimmableLightRemote.class;
    public static final Class<? extends DimmerRemote> DAL_UNIT_DIMMER = DimmerRemote.class;
    public static final Class<? extends MotionDetectorRemote> DAL_UNIT_MOTION_DETECTOR = MotionDetectorRemote.class;
    public static final Class<? extends PowerSwitchRemote> DAL_UNIT_POWER_SWITCH = PowerSwitchRemote.class;
    public static final Class<? extends PowerConsumptionSensorRemote> DAL_UNIT_POWER_CONSUMPTION_SENSOR = PowerConsumptionSensorRemote.class;
    public static final Class<? extends ButtonRemote> DAL_UNIT_BUTTON = ButtonRemote.class;
    public static final Class<? extends TemperatureControllerRemote> DAL_UNIT_TEMPERATURE_CONTROLLER = TemperatureControllerRemote.class;
    public static final Class<? extends TemperatureSensorRemote> DAL_UNIT_TEMPERATURE_SENSOR = TemperatureSensorRemote.class;
    public static final Class<? extends BatteryRemote> DAL_UNIT_BATTERY = BatteryRemote.class;
    public static final Class<? extends HandleRemote> DAL_UNIT_HANDLE = HandleRemote.class;
    public static final Class<? extends MonitorRemote> DAL_UNIT_MONITOR = MonitorRemote.class;
    public static final Class<? extends ReedContactRemote> DAL_UNIT_REED_CONTACT = ReedContactRemote.class;
    public static final Class<? extends RollerShutterRemote> DAL_UNIT_ROLLER_SHUTTER = RollerShutterRemote.class;
    public static final Class<? extends SmokeDetectorRemote> DAL_UNIT_SMOKE_DETECTOR = SmokeDetectorRemote.class;
    public static final Class<? extends TamperDetectorRemote> DAL_UNIT_TAMPER_DETECTOR = TamperDetectorRemote.class;
    // comfort
    public static final Class<? extends LightRemote> UNIT_DAL_LIGHT = DAL_UNIT_LIGHT;
    public static final Class<? extends LightSensorRemote> UNIT_DAL_LIGHT_SENSOR = DAL_UNIT_LIGHT_SENSOR;
    public static final Class<? extends ColorableLightRemote> UNIT_DAL_LIGHT_COLORABLE = DAL_UNIT_COLORABLE_LIGHT;
    public static final Class<? extends DimmableLightRemote> UNIT_DAL_LIGHT_DIMMABLE = DAL_UNIT_DIMMABLE_LIGHT;
    public static final Class<? extends DimmerRemote> UNIT_DAL_DIMMER = DAL_UNIT_DIMMER;
    public static final Class<? extends MotionDetectorRemote> UNIT_DAL_MOTION_DETECTOR = DAL_UNIT_MOTION_DETECTOR;
    public static final Class<? extends PowerSwitchRemote> UNIT_DAL_POWER_SWITCH = DAL_UNIT_POWER_SWITCH;
    public static final Class<? extends PowerConsumptionSensorRemote> UNIT_DAL_POWER_CONSUMPTION_SENSOR = DAL_UNIT_POWER_CONSUMPTION_SENSOR;
    public static final Class<? extends ButtonRemote> UNIT_DAL_BUTTON = DAL_UNIT_BUTTON;
    public static final Class<? extends TemperatureControllerRemote> UNIT_DAL_TEMPERATURE_CONTROLLER = DAL_UNIT_TEMPERATURE_CONTROLLER;
    public static final Class<? extends TemperatureSensorRemote> UNIT_DAL_TEMPERATURE_SENSOR = DAL_UNIT_TEMPERATURE_SENSOR;
    public static final Class<? extends BatteryRemote> UNIT_DAL_BATTERY = DAL_UNIT_BATTERY;
    public static final Class<? extends HandleRemote> UNIT_DAL_HANDLE = DAL_UNIT_HANDLE;
    public static final Class<? extends MonitorRemote> UNIT_DAL_MONITOR = DAL_UNIT_MONITOR;
    public static final Class<? extends ReedContactRemote> UNIT_DAL_REED_CONTACT = DAL_UNIT_REED_CONTACT;
    public static final Class<? extends RollerShutterRemote> UNIT_DAL_ROLLER_SHUTTER = DAL_UNIT_ROLLER_SHUTTER;
    public static final Class<? extends SmokeDetectorRemote> UNIT_DAL_SMOKE_DETECTOR = DAL_UNIT_SMOKE_DETECTOR;
    public static final Class<? extends TamperDetectorRemote> UNIT_DAL_TAMPER_DETECTOR = DAL_UNIT_TAMPER_DETECTOR;
    //simple
    public static final Class<? extends LightRemote> LIGHT = DAL_UNIT_LIGHT;
    public static final Class<? extends LightSensorRemote> LIGHT_SENSOR = DAL_UNIT_LIGHT_SENSOR;
    public static final Class<? extends ColorableLightRemote> LIGHT_COLORABLE = DAL_UNIT_COLORABLE_LIGHT;
    public static final Class<? extends ColorableLightRemote> COLORABLE_LIGHT = DAL_UNIT_COLORABLE_LIGHT;
    public static final Class<? extends DimmableLightRemote> LIGHT_DIMMABLE = DAL_UNIT_DIMMABLE_LIGHT;
    public static final Class<? extends DimmableLightRemote> DIMMABLE_LIGHT = DAL_UNIT_DIMMABLE_LIGHT;
    public static final Class<? extends DimmerRemote> DIMMER = DAL_UNIT_DIMMER;
    public static final Class<? extends MotionDetectorRemote> MOTION_DETECTOR = DAL_UNIT_MOTION_DETECTOR;
    public static final Class<? extends PowerSwitchRemote> POWER_SWITCH = DAL_UNIT_POWER_SWITCH;
    public static final Class<? extends PowerConsumptionSensorRemote> POWER_CONSUMPTION_SENSOR = DAL_UNIT_POWER_CONSUMPTION_SENSOR;
    public static final Class<? extends ButtonRemote> BUTTON = DAL_UNIT_BUTTON;
    public static final Class<? extends TemperatureControllerRemote> TEMPERATURE_CONTROLLER = DAL_UNIT_TEMPERATURE_CONTROLLER;
    public static final Class<? extends TemperatureSensorRemote> TEMPERATURE_SENSOR = DAL_UNIT_TEMPERATURE_SENSOR;
    public static final Class<? extends BatteryRemote> BATTERY = DAL_UNIT_BATTERY;
    public static final Class<? extends HandleRemote> HANDLE = DAL_UNIT_HANDLE;
    public static final Class<? extends MonitorRemote> MONITOR = DAL_UNIT_MONITOR;
    public static final Class<? extends ReedContactRemote> REED_CONTACT = DAL_UNIT_REED_CONTACT;
    public static final Class<? extends RollerShutterRemote> ROLLER_SHUTTER = DAL_UNIT_ROLLER_SHUTTER;
    public static final Class<? extends SmokeDetectorRemote> SMOKE_DETECTOR = DAL_UNIT_SMOKE_DETECTOR;
    public static final Class<? extends TamperDetectorRemote> TAMPER_DETECTOR = DAL_UNIT_TAMPER_DETECTOR;

    private static final Logger LOGGER = LoggerFactory.getLogger(Units.class);
    private static final ReentrantReadWriteLock UNIT_REMOTE_REGISTRY_LOCK = new ReentrantReadWriteLock();
    private static final UnitRemoteFactory UNIT_REMOTE_FACTORY = UnitRemoteFactoryImpl.getInstance();
    // register an observer that calls shutdown on units whose configs have been removed from the registry
    private static final ProtobufListDiff<String, UnitConfig, UnitConfig.Builder> UNIT_DIFF = new ProtobufListDiff<>();
    //    public static final Class<? extends VideoRgbSourceRemote> VIDEO_RGB_SOURCE = VideoRgbSourceRemote.class;
//    public static final Class<? extends VideoDepthSourceRemote> VIDEO_DEPTH_SOURCE = VideoDepthSourceRemote.class;
//    public static final Class<? extends AudioSourceRemote> AUDIO_SOURCE = AudioSourceRemote.class;
    public static Units instance;
    private static RemoteControllerRegistry<String, org.openbase.bco.dal.lib.layer.unit.UnitRemote<? extends Message>> unitRemoteRegistry;
    private static final Observer<DataProvider<UnitRegistryData>, UnitRegistryData> UNIT_REGISTRY_OBSERVER = new Observer<DataProvider<UnitRegistryData>, UnitRegistryData>() {
        @Override
        public void update(DataProvider<UnitRegistryData> source, UnitRegistryData data) throws Exception {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().lock();
            UNIT_DIFF.diffMessages(Registries.getUnitRegistry().getUnitConfigs());
            try {
                for (String unitId : UNIT_DIFF.getRemovedMessageMap().keySet()) {
                    if (unitRemoteRegistry.contains(unitId)) {
                        removeUnitRemote(unitRemoteRegistry.get(unitId));
                    }
                }
            } finally {
                UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock();
            }
        }
    };

    private static boolean shutdownInitialized = false;

    static {
        try {
            unitRemoteRegistry = new RemoteControllerRegistry<>();
            Shutdownable.registerShutdownHook(new Shutdownable() {
                @Override
                public void shutdown() {
                    try {
                        shutdownInitialized = true;
                        for (UnitRemote<? extends Message> unitRemote : unitRemoteRegistry.getEntries()) {
                            try {
                                unitRemote.unlock(unitRemoteRegistry);
                                unitRemote.shutdown();
                            } catch (CouldNotPerformException ex) {
                                ExceptionPrinter.printHistory("Could not properly shutdown " + unitRemote, ex, LOGGER);
                            }
                        }
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory("Could not properly shutdown remote pool!", ex, LOGGER);
                    } finally {
                        unitRemoteRegistry.shutdown();
                    }
                }

                @Override
                public String toString() {
                    return "UnitRemotePool";
                }
            });

            Registries.getUnitRegistry().addDataObserver(UNIT_REGISTRY_OBSERVER);
            if (Registries.getUnitRegistry().isDataAvailable()) {
                UNIT_DIFF.diffMessages(Registries.getUnitRegistry().getUnitConfigs());
            }
        } catch (CouldNotPerformException ex) {
            if(!ExceptionProcessor.isCausedBySystemShutdown(ex)) {
                ExceptionPrinter.printHistory(new FatalImplementationErrorException(Units.class, new org.openbase.jul.exception.InstantiationException(Units.class, ex)), LOGGER);
            }
        }
    }

    /**
     * Reset the unit remote pool by shutting down every remote instance and
     * clearing the local registry. This is needed for unit tests because else
     * remote states are not cleared between tests which can result in
     * unpredictable values. This method can only be triggered in test mode.
     * Else it throws a FatalImplementationErrorException.
     *
     * @param responsibleObject should be the object which triggers the reset
     *                          which is used for proper exception handling
     *
     * @throws CouldNotPerformException if reset fails
     */
    public static void reset(final Object responsibleObject) throws CouldNotPerformException {
        try {
            if (!JPService.testMode()) {
                throw new FatalImplementationErrorException("Units can only be reseted in test mode!", responsibleObject);
            }

            UNIT_REMOTE_REGISTRY_LOCK.writeLock().lock();
            try {
                for (UnitRemote unitRemote : new ArrayList<>(unitRemoteRegistry.getEntries())) {
                    removeUnitRemote(unitRemote);
                }
            } finally {
                UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock();
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not reset Units!", ex);
        }
    }

    private static void removeUnitRemote(final UnitRemote unitRemote) {
        try {
            unitRemoteRegistry.remove(unitRemote);
            unitRemote.unlock(unitRemoteRegistry);
            unitRemote.shutdown();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not properly shutdown " + unitRemote, ex, LOGGER);
        }
    }

    /**
     * Method forces a resynchronization on all unit remotes.
     *
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    public static void reinitialize() throws CouldNotPerformException, InterruptedException {
        UNIT_REMOTE_REGISTRY_LOCK.writeLock().lock();
        try {
            for (UnitRemote unitRemote : unitRemoteRegistry.getEntries()) {
                try {
                    unitRemote.unlock(unitRemoteRegistry);
                    unitRemote.init(unitRemote.getConfig());
                    unitRemote.lock(unitRemoteRegistry);
                    unitRemote.requestData().get(500, TimeUnit.MILLISECONDS);
                } catch (ExecutionException | TimeoutException ex) {
                    throw new CouldNotPerformException("Could not reinitialize Units");
                }
            }
        } finally {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock();
        }

        resetUnitRegistryObserver();
    }

    /**
     * Method casts the given unit remote to the given unit remote class type if possible.
     *
     * @param unitRemote      the unit remote to cast.
     * @param unitRemoteClass the type to cast
     * @param <UR>            the unit remote type
     *
     * @return the casted remote instance
     *
     * @throws VerificationFailedException is thrown if the given instance is not compatible with the given remote class type.
     */
    private static <UR extends UnitRemote<?>> UR castUnitRemote(final UnitRemote<?> unitRemote, final Class<UR> unitRemoteClass) throws VerificationFailedException {
        // check is needed because java does not care about invalid generic casts.
        if (!unitRemoteClass.isInstance(unitRemote)) {
            throw new VerificationFailedException("Remote[" + unitRemote + "] is not compatible!");
        }
        return (UR) unitRemote;
    }

    /**
     * Reset the unit registry observer by removing it reseting the unit diff and adding the observer again.
     * This is needed in unit tests because if multiple tests are run and in theses tests
     * the MockRegistry is restarted then the observer is registered on an old instance of the unit remote
     * and thus the effects of the observer cannot be tested.
     *
     * @throws org.openbase.jul.exception.CouldNotPerformException thrown if the unit registry is not available
     */
    private static void resetUnitRegistryObserver() throws CouldNotPerformException {
        Registries.getUnitRegistry().removeDataObserver(UNIT_REGISTRY_OBSERVER);
        UNIT_DIFF.replaceOriginalMap(new IdentifiableMessageMap<>(Registries.getUnitRegistry().getDalUnitConfigs()));
        Registries.getUnitRegistry().addDataObserver(UNIT_REGISTRY_OBSERVER);
    }

    /**
     * Method returns an instance of the unit registry. If this method is called
     * for the first time, a new registry instance will be created. This method
     * only returns if the unit registry synchronization is finished otherwise
     * this method blocks forever. In case you won't wait for the
     * synchronization, request your own instance by calling:
     * <code>Registries.getUnitRegistry();</code>
     *
     * @return the unit registry instance.
     *
     * @throws InterruptedException     is thrown if the current thread was
     *                                  externally interrupted.
     * @throws CouldNotPerformException Is thrown in case an error occurs during
     *                                  registry connection.
     */
    public synchronized static UnitRegistry getUnitRegistry() throws InterruptedException, CouldNotPerformException {
        Registries.waitForData();
        return Registries.getUnitRegistry();
    }

    /**
     * Returns the unit remote of the unit identified by the given unit id.
     *
     * @param unitId the unit id to identify the unit.
     *
     * @return a new created or already cached unit remote.
     *
     * @throws NotAvailableException is thrown if the unit remote was not
     *                               available.
     * @throws InterruptedException  is thrown if the current thread was
     *                               externally interrupted.
     */
    private static UnitRemote<?> getUnitRemote(final String unitId) throws NotAvailableException, InterruptedException {
        final boolean newInstance;
        final UnitRemote<?> unitRemote;
        try {

            if (shutdownInitialized) {
                throw new ShutdownInProgressException("UnitPool");
            }

            UNIT_REMOTE_REGISTRY_LOCK.writeLock().lock();
            try {
                if (!unitRemoteRegistry.contains(unitId)) {
                    // create new instance.
                    newInstance = true;
                    final UnitConfig unitConfig = getUnitRegistry().getUnitConfigById(unitId);
                    UnitConfigProcessor.verifyEnablingState(unitConfig);
                    unitRemote = UNIT_REMOTE_FACTORY.newInitializedInstance(unitConfig);
                    unitRemoteRegistry.register(unitRemote);

                } else {
                    // return cached instance.
                    newInstance = false;
                    unitRemote = unitRemoteRegistry.get(unitId);
                }
            } finally {
                UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock();
            }

            // The activation is not synchronized by the unitRemoteRegistryLock out of performance reasons. 
            // By this, new unit remotes can be requested independent from the activation state of other units.
            if (newInstance) {
                unitRemote.activate();
                unitRemote.lock(unitRemoteRegistry);
            }
            // set sessionManager in unit remote
            unitRemote.setSessionManager(SessionManager.getInstance());
            return unitRemote;
        } catch (CouldNotPerformException | NullPointerException ex) {
            throw new NotAvailableException("UnitRemote[" + unitId + "]", ex);
        }
    }


    /**
     * Returns the unit remote of the unit identified by the given unit config.
     *
     * @param unitConfig the unit config to identify the unit.
     *
     * @return a new created or already cached unit remote.
     *
     * @throws NotAvailableException is thrown if the unit remote was not
     *                               available.
     * @throws InterruptedException  is thrown if the current thread was
     *                               externally interrupted.
     */
    private static UnitRemote<?> getUnitRemote(final UnitConfig unitConfig) throws NotAvailableException, InterruptedException {
        final boolean newInstance;
        final UnitRemote unitRemote;
        try {
            UNIT_REMOTE_REGISTRY_LOCK.writeLock().lock();
            try {
                if (!unitRemoteRegistry.contains(unitConfig.getId())) {
                    // create new instance.
                    newInstance = true;
                    unitRemote = UNIT_REMOTE_FACTORY.newInitializedInstance(unitConfig);
                    unitRemoteRegistry.register(unitRemote);

                } else {
                    // return cached instance.
                    newInstance = false;
                    unitRemote = unitRemoteRegistry.get(unitConfig.getId());
                }

                if (newInstance && unitRemote.isEnabled()) {
                    unitRemote.activate(unitRemoteRegistry);
                }
                // set sessionManager in unit remote
                unitRemote.setSessionManager(SessionManager.getInstance());
            } finally {
                UNIT_REMOTE_REGISTRY_LOCK.writeLock().unlock();
            }
            return unitRemote;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UnitRemote[" + unitConfig.getId() + "]", ex);
        }
    }

    /**
     * Method waits for unit data if the waitForData flag is set.
     *
     * @param unitRemote  the remote to wait for data.
     * @param waitForData the flag to decide if the current thread should be
     *                    wait for the uni data.
     *
     * @return the given unit remote.
     *
     * @throws CouldNotPerformException is thrown if any error occurs during the
     *                                  wait phase.
     * @throws InterruptedException     is thrown in case the thread is externally
     *                                  interrupted.
     */
    private static UnitRemote<?> waitForData(final UnitRemote<?> unitRemote, final boolean waitForData) throws CouldNotPerformException, InterruptedException {
        if (waitForData) {
            unitRemote.waitForData();
        }
        return unitRemote;
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * identifier. The returned unit remote object is fully synchronized with
     * the unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param unitId      the unit identifier.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not
     *                               available.
     * @throws InterruptedException  is thrown in case the thread is externally
     *                               interrupted
     */
    public static UnitRemote<?> getUnit(final String unitId, final boolean waitForData) throws NotAvailableException, InterruptedException {
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
     * Method establishes a connection to the unit referred by the given unit
     * identifier. The returned unit remote object is fully synchronized with
     * the unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param unitConfig  the unit configuration.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not
     *                               available.
     * @throws InterruptedException  is thrown in case the thread is externally
     *                               interrupted
     */
    public static UnitRemote<?> getUnit(final UnitConfig unitConfig, final boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (unitConfig == null) {
                assert false;
                throw new NotAvailableException("UnitConfig");
            }
            return waitForData(getUnitRemote(unitConfig), waitForData);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + unitConfig.getId() + "|" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "]", ex);
        }
    }

    /**
     * This method is a wrapper for
     * {@link #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     * and casts the result to the given {@code unitRemoteClass}.
     *
     * @param <UR>            the unit remote class type.
     * @param unitConfig      Checkout wrapped method doc
     *                        {@link #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     * @param waitForData     Checkout wrapped method doc
     *                        {@link #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     * @param unitRemoteClass the unit remote class.
     *
     * @return an instance of the given remote class.
     *
     * @throws NotAvailableException Is thrown if the remote instance is not
     *                               compatible with the given class. See
     *                               {{@link #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     *                               for further cases.
     * @throws InterruptedException  Checkout wrapped method doc
     *                               {@link #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     * @see #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean)
     */
    public static <UR extends UnitRemote<?>> UR getUnit(final UnitConfig unitConfig, final boolean waitForData, final Class<UR> unitRemoteClass) throws NotAvailableException, InterruptedException {
        try {
            return castUnitRemote(getUnit(unitConfig, waitForData), unitRemoteClass);
        } catch (ClassCastException | VerificationFailedException ex) {
            throw new NotAvailableException("Unit[" + unitConfig.getId() + "]", new InvalidStateException("Requested Unit[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "] of UnitType[" + unitConfig.getUnitType() + "] is not compatible with defined UnitRemoteClass[" + unitRemoteClass + "]!", ex));
        }
    }

    /**
     * This method is a wrapper for
     * {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)} and
     * casts the result to the given {@code unitRemoteClass}.
     *
     * @param <UR>            the unit remote class type.
     * @param unitId          Checkout wrapped method doc
     *                        {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)}
     * @param waitForData     Checkout wrapped method doc
     *                        {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)}
     * @param unitRemoteClass the unit remote class.
     *
     * @return an instance of the given remote class.
     *
     * @throws NotAvailableException Is thrown if the remote instance is not
     *                               compatible with the given class. See
     *                               {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)} for
     *                               further cases.
     * @throws InterruptedException  Checkout wrapped method doc
     *                               {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)}
     * @see #getUnit(java.lang.String, boolean)
     */
    public static <UR extends UnitRemote<?>> UR getUnit(final String unitId, boolean waitForData, final Class<UR> unitRemoteClass) throws NotAvailableException, InterruptedException {
        try {
            return castUnitRemote(getUnit(unitId, waitForData), unitRemoteClass);
        } catch (ClassCastException | VerificationFailedException ex) {
            throw new NotAvailableException("Unit[" + unitId + "]", new InvalidStateException("Requested Unit[" + unitId + "] is not compatible with defined UnitRemoteClass[" + unitRemoteClass + "]!", ex));
        }
    }

    /**
     * Method establishes a connection to all enabled units.
     * The returned unit remote objects are fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     * <p>
     * Note: Please to not use this method if you not really need all units because this method establishes a connection to each unit and the synchronization will even continue if you are not working with the remote instances anymore.
     *
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a list of new or cached unit remotes which can be used to control the units or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException  is thrown in case the thread is externally interrupted.
     */
    public static List<UnitRemote<?>> getUnits(boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            final ArrayList<UnitRemote<?>> unitRemoteList = new ArrayList<>();
            for (UnitConfig unitConfig : getUnitRegistry().getUnitConfigs()) {
                if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                    continue;
                }
                unitRemoteList.add(getUnit(unitConfig, waitForData));
            }
            return unitRemoteList;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Units", ex);
        }
    }


    /**
     * Method establishes a connection to the units referred by the given unit label.
     * The returned unit remote objects are fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param label       the label to identify the unit.
     * @param unitType    the type of the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a list of new or cached unit remotes which can be used to control the units or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException  is thrown in case the thread is externally interrupted.
     */
    public static List<UnitRemote<?>> getUnitsByLabelAndType(final String label, final UnitType unitType, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (label == null) {
                assert false;
                throw new NotAvailableException("UnitName");
            }

            final ArrayList<UnitRemote<?>> unitRemoteList = new ArrayList<>();
            for (UnitConfig unitConfig : getUnitRegistry().getUnitConfigsByLabelAndUnitType(label, unitType)) {
                unitRemoteList.add(getUnit(unitConfig, waitForData));
            }
            return unitRemoteList;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + label + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit label.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param label       the label to identify the unit.
     * @param unitType    the type of the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException  is thrown in case the thread is externally interrupted.
     * @deprecated since v2.0 and will be removed in v3.0. Please use getUnitsByLabelAndType(...) instead.
     */
    @Deprecated
    public static UnitRemote<?> getUnitByLabelAndType(final String label, final UnitType unitType, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (label == null) {
                assert false;
                throw new NotAvailableException("UnitName");
            }
            final List<UnitConfigType.UnitConfig> unitConfigList = getUnitRegistry().getUnitConfigsByLabelAndUnitType(label, unitType);

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
     * Method establishes a connection to the units referred by the given unit label.
     * The returned unit remote objects are fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param label       the label to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a list of new or cached unit remotes which can be used to control the units or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException  is thrown in case the thread is externally interrupted.
     */
    public static List<UnitRemote<?>> getUnitsByLabel(final String label, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (label == null) {
                assert false;
                throw new NotAvailableException("UnitName");
            }

            final ArrayList<UnitRemote<?>> unitRemoteList = new ArrayList<>();
            for (UnitConfig unitConfig : getUnitRegistry().getUnitConfigsByLabel(label)) {
                unitRemoteList.add(getUnit(unitConfig, waitForData));
            }
            return unitRemoteList;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + label + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit alias.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param alias       the alias to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException  is thrown in case the thread is externally interrupted.
     */
    public static UnitRemote<?> getUnitByAlias(final String alias, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (alias == null) {
                assert false;
                throw new NotAvailableException("UnitAlias");
            }
            return getUnit(getUnitRegistry().getUnitConfigByAlias(alias), waitForData);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit", "alias:" + alias, ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit alias.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param alias           the alias to identify the unit.
     * @param waitForData     if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     * @param unitRemoteClass the unit remote class.
     *
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException  is thrown in case the thread is externally interrupted.
     */
    public static <UR extends UnitRemote<?>> UR getUnitByAlias(final String alias, boolean waitForData, final Class<UR> unitRemoteClass) throws NotAvailableException, InterruptedException {
        try {
            if (alias == null) {
                assert false;
                throw new NotAvailableException("UnitAlias");
            }
            return castUnitRemote(getUnit(getUnitRegistry().getUnitConfigByAlias(alias), waitForData), unitRemoteClass);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit", "alias:" + alias, ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit alias.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param alias       the alias to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     */
    public static Future<UnitRemote<?>> getFutureUnitByAlias(final String alias, final boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnitByAlias(alias, waitForData));
    }

    /**
     * Method establishes a connection to the unit referred by the given unit alias.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param alias           the alias to identify the unit.
     * @param waitForData     if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     * @param unitRemoteClass the unit remote class.
     *
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     */
    public static <UR extends UnitRemote<?>> Future<UR> getFutureUnitByAlias(final String alias, final boolean waitForData, final Class<UR> unitRemoteClass) {
        return GlobalCachedExecutorService.submit(() -> getUnitByAlias(alias, waitForData, unitRemoteClass));
    }

    /**
     * Method establishes a connection to the unit referred by the given unit label.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param label       the label to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available or the label is not unique enough to identify the unit.
     * @throws InterruptedException  is thrown in case the thread is externally interrupted.
     * @deprecated since v2.0 and will be removed in v3.0. Please use getUnitsByLabel(..) instead.
     */
    @Deprecated
    public static UnitRemote<?> getUnitByLabel(final String label, boolean waitForData) throws NotAvailableException, InterruptedException {
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
     * This method is a wrapper for {@link #getUnit(java.lang.String, boolean) getUnitsByLabel(String, boolean)} and casts the result to the given {@code unitRemoteClass} list.
     *
     * @param <UR>            the unit remote class type.
     * @param label           Checkout wrapped method doc {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @param waitForData     Checkout wrapped method doc {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @param unitRemoteClass the unit remote class.
     *
     * @return a list of instances of the given remote class.
     *
     * @throws NotAvailableException Is thrown if the remote instance is not compatible with the given class. See {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)} for further cases.
     * @throws InterruptedException  Checkout wrapped method doc {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @see #getUnitsByLabel(java.lang.String, boolean)
     */
    public static <UR extends UnitRemote<?>> List<UR> getUnitsByLabel(final String label, boolean waitForData, final Class<UR> unitRemoteClass) throws NotAvailableException, InterruptedException {
        try {
            try {
                return (List<UR>) getUnitsByLabelAndType(label, Units.getUnitTypeByRemoteClass(unitRemoteClass), waitForData);
            } catch (ClassCastException ex) {
                throw new InvalidStateException("Requested Unit[" + label + "] is not compatible with defined UnitRemoteClass[" + unitRemoteClass + "]!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + label + "]", ex);
        }
    }

    /**
     * This method is a wrapper for {@link #getUnit(java.lang.String, boolean) getUnitsByLabel(String, boolean)} and casts the result to the given {@code unitRemoteClass} list.
     *
     * @param <UR>            the unit remote class type.
     * @param label           Checkout wrapped method doc
     *                        {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @param waitForData     Checkout wrapped method doc
     *                        {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @param unitRemoteClass the unit remote class.
     *
     * @return an instance of the given remote class.
     *
     * @throws NotAvailableException Is thrown if the remote instance is not
     *                               compatible with the given class. See
     *                               {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)} for
     *                               further cases.
     * @throws InterruptedException  Checkout wrapped method doc
     *                               {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @see #getUnitByLabel(java.lang.String, boolean)
     * @deprecated since v2.0 and will be removed in v3.0. Please use getUnitsByLabel(...) instead.
     */
    @Deprecated
    public static <UR extends UnitRemote<?>> UR getUnitByLabel(final String label, boolean waitForData, final Class<UR> unitRemoteClass) throws NotAvailableException, InterruptedException {
        try {
            try {
                return castUnitRemote(getUnitsByLabelAndType(label, Units.getUnitTypeByRemoteClass(unitRemoteClass), waitForData).get(0), unitRemoteClass);
            } catch (ClassCastException | VerificationFailedException ex) {
                throw new InvalidStateException("Requested Unit[" + label + "] is not compatible with defined UnitRemoteClass[" + unitRemoteClass + "]!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + label + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * scope. The returned unit remote object is fully synchronized with the
     * unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param scope       the scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available
     *                               or the label is not unique enough to identify the unit.
     * @throws InterruptedException  is thrown in case the thread is externally
     *                               interrupted.
     */
    public static UnitRemote<?> getUnitByScope(final ScopeType.Scope scope, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (scope == null) {
                assert false;
                throw new NotAvailableException("UnitScope");
            }
            return getUnit(getUnitRegistry().getUnitConfigByScope(scope), waitForData);
        } catch (CouldNotPerformException ex) {
            try {
                throw new NotAvailableException("Unit[" + ScopeProcessor.generateStringRep(scope) + "]", ex);
            } catch (CouldNotPerformException ex1) {
                throw new NotAvailableException("Unit[?]", ex);
            }
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * scope. The returned unit remote object is fully synchronized with the
     * unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param scope       the scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available.
     * @throws InterruptedException  is thrown in case the thread is externally
     *                               interrupted.
     */
    public static UnitRemote<?> getUnitByScope(final Scope scope, boolean waitForData) throws NotAvailableException, InterruptedException {
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
     * Method establishes a connection to the unit referred by the given unit
     * scope. The returned unit remote object is fully synchronized with the
     * unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param scope       the scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available.
     * @throws InterruptedException  is thrown in case the thread is externally
     *                               interrupted.
     */
    public static UnitRemote<?> getUnitByScope(final String scope, boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            if (scope == null) {
                assert false;
                throw new NotAvailableException("UnitScope");
            }
            return getUnitByScope(ScopeProcessor.generateScope(scope), waitForData);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + scope + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * identifier. The returned unit remote object is fully synchronized with
     * the unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param unitId      the unit identifier.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     */
    public static Future<UnitRemote<?>> getFutureUnit(final String unitId, final boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnit(unitId, waitForData));
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * identifier. The returned unit remote object is fully synchronized with
     * the unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param unitConfig  the unit configuration.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     */
    public static Future<UnitRemote<?>> getFutureUnit(final UnitConfig unitConfig, final boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnit(unitConfig, waitForData));
    }

    /**
     * This method is a wrapper for
     * {@link #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     * and casts the result to the given {@code unitRemoteClass}.
     *
     * @param <UR>            the unit remote class type.
     * @param unitConfig      Checkout wrapped method doc
     *                        {@link #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     * @param waitForData     Checkout wrapped method doc
     *                        {@link #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean) getUnit(UnitConfig, boolean)}
     * @param unitRemoteClass the unit remote class.
     *
     * @return an instance of the given remote class.
     *
     * @see #getUnit(org.openbase.type.domotic.unit.UnitConfigType.UnitConfig, boolean)
     */
    public static <UR extends UnitRemote<?>> Future<UR> getFutureUnit(final UnitConfig unitConfig, final boolean waitForData, final Class<UR> unitRemoteClass) {
        return GlobalCachedExecutorService.submit(() -> getUnit(unitConfig, waitForData, unitRemoteClass));
    }

    /**
     * This method is a wrapper for
     * {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)} and
     * casts the result to the given {@code unitRemoteClass}.
     *
     * @param <UR>            the unit remote class type.
     * @param unitId          Checkout wrapped method doc
     *                        {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)}
     * @param waitForData     Checkout wrapped method doc
     *                        {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)}
     * @param unitRemoteClass the unit remote class.
     *
     * @return an instance of the given remote class.
     *
     * @see #getUnit(java.lang.String, boolean)
     */
    public static <UR extends UnitRemote<?>> Future<UR> getFutureUnit(final String unitId, boolean waitForData, final Class<UR> unitRemoteClass) {
        return GlobalCachedExecutorService.submit(() -> getUnit(unitId, waitForData, unitRemoteClass));
    }

    /**
     * Method establishes a connection to all enabled units.
     * The returned unit remote objects are fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     * <p>
     * Note: Please to not use this method if you not really need all units because this method establishes a connection to each unit and the synchronization will even continue if you are not working with the remote instances anymore.
     *
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a list of new or cached unit remotes which can be used to control the units or request all current unit states.
     */
    public static Future<List<UnitRemote<?>>> getFutureUnits(boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnits(waitForData));
    }

    /**
     * Method establishes connections to the units referred by the given unit label.
     * The returned unit remote objects are fully synchronized with the unit controllers and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remotes are fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param label       the label to identify the unit.
     * @param unitType    the type of the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a list of new or cached unit remotes which can be used to control the units or request all current unit states.
     */
    public static Future<List<UnitRemote<?>>> getFutureUnitsByLabelAndType(final String label, final UnitType unitType, boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnitsByLabelAndType(label, unitType, waitForData));
    }

    /**
     * Method establishes connections to the units referred by the given unit label.
     * The returned unit remote objects are fully synchronized with the unit controllers and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remotes are fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param label       the label to identify the unit.
     * @param waitForData if this flag is set to true the current thread will block until the unit remote is fully synchronized with the unit controller.
     *
     * @return a list of new or cached unit remotes which can be used to control the units or request all current unit states.
     */
    public static Future<List<UnitRemote<?>>> getFutureUnitsByLabel(final String label, boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnitsByLabel(label, waitForData));
    }

    /**
     * This method is a wrapper for {@link #getUnitsByLabel(java.lang.String, boolean)} and casts the result to the given {@code unitRemoteClass} list.
     *
     * @param <UR>            the unit remote class type.
     * @param label           Checkout wrapped method doc {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @param waitForData     Checkout wrapped method doc {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @param unitRemoteClass the unit remote class.
     *
     * @return a list of instances of the given remote class.
     *
     * @see #getUnitsByLabel(java.lang.String, boolean)
     */
    public static <UR extends UnitRemote<?>> Future<List<UR>> getFutureUnitsByLabel(final String label, boolean waitForData, final Class<UR> unitRemoteClass) {
        return GlobalCachedExecutorService.submit(() -> getUnitsByLabel(label, waitForData, unitRemoteClass));
    }

    /**
     * Method establishes a connection to the unit referred by the given unit label.
     * The returned unit remote object is fully synchronized with the unit controller and all states locally cached.
     * Use the {@code waitForData} flag to block the current thread until the unit remote is fully synchronized with the unit controller during the startup phase.
     * This synchronization is just done once and the current thread will not block if the unit remote was already synchronized before.
     * To force a resynchronization call {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the remote instance.
     * Please avoid polling unit states! If you want to get informed about unit config or unit data state changes, please register new config or data observer on this remote instance.
     *
     * @param label       the label to identify the unit.
     * @param unitType    the type of the unit.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     */
    @Deprecated
    public static Future<UnitRemote> getFutureUnitByLabelAndType(final String label, final UnitType unitType, boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnitByLabelAndType(label, unitType, waitForData));
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * label. The returned unit remote object is fully synchronized with the
     * unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param label       the label to identify the unit.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit state.
     */
    @Deprecated
    public static Future<UnitRemote> getFutureUnitByLabel(final String label, boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnitByLabel(label, waitForData));
    }

    /**
     * This method is a wrapper for
     * {@link #getUnit(java.lang.String, boolean) getUnit(String, boolean)} and
     * casts the result to the given {@code unitRemoteClass}.
     *
     * @param <UR>            the unit remote class type.
     * @param label           Checkout wrapped method doc
     *                        {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @param waitForData     Checkout wrapped method doc
     *                        {@link #getUnitByLabel(java.lang.String, boolean) getUnit(String, boolean)}
     * @param unitRemoteClass the unit remote class.
     *
     * @return an instance of the given remote class.
     *
     * @see #getUnitByLabel(java.lang.String, boolean)
     */
    @Deprecated
    public static <UR extends UnitRemote<?>> Future<UR> getFutureUnitByLabel(final String label, boolean waitForData, final Class<UR> unitRemoteClass) {
        return GlobalCachedExecutorService.submit(() -> getUnitByLabel(label, waitForData, unitRemoteClass));
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * scope. The returned unit remote object is fully synchronized with the
     * unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param scope       the scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     */
    public static Future<UnitRemote<?>> getFutureUnitByScope(final ScopeType.Scope scope, boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnitByScope(scope, waitForData));
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * scope. The returned unit remote object is fully synchronized with the
     * unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param scope       the scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     */
    public static Future<UnitRemote<?>> getFutureUnitByScope(final Scope scope, boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnitByScope(scope, waitForData));
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * scope. The returned unit remote object is fully synchronized with the
     * unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param scope       the scope to identify the unit.
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     */
    public static Future<UnitRemote<?>> getFutureUnitByScope(final String scope, boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnitByScope(scope, waitForData));
    }

    /**
     * Method resolves the UnitType of the given unit remote class.
     *
     * @param unitRemoteClass the unit remote class to resolve the unit type.
     *
     * @return the detected UnitType
     *
     * @throws CouldNotPerformException is thrown in case if the UnitType could
     *                                  not be resolved.
     */
    public static UnitType getUnitTypeByRemoteClass(final Class<? extends UnitRemote<?>> unitRemoteClass) throws CouldNotPerformException {
        try {
            return UnitType.valueOf(StringProcessor.transformToUpperCase(unitRemoteClass.getSimpleName().replaceAll("Remote", "")));
        } catch (NullPointerException | IllegalArgumentException ex) {
            throw new CouldNotPerformException("Could not resolve unit type out of UnitRemoteClass[" + unitRemoteClass.getName() + "]", ex);
        }
    }

    /**
     * Method checks if the given unitRemote is managed by this unit remote
     * pool.
     *
     * @param unitRemote the remote to check.
     *
     * @return true if the given remote is managed by this pool otherwise false
     * is returned.
     *
     * @throws CouldNotPerformException is thrown if the given remote is not
     *                                  valid. For instance if the given unit remote is not initialized.
     */
    public static boolean contains(final UnitRemote<? extends Message> unitRemote) throws CouldNotPerformException {
        return unitRemoteRegistry.contains(unitRemote);
    }

    /**
     * Method returns the transformation leading from the given unit to the root location.
     *
     * @param unitConfig the unit where the transformation leads to.
     *
     * @return a transformation future
     */
    public static Future<Transform> getUnitToRootTransformation(final UnitConfig unitConfig) {
        final Future<UnitRegistryData> dataFuture;
        try {
            dataFuture = Registries.getUnitRegistry().getDataFuture();
            return FutureProcessor.allOfInclusiveResultFuture(Registries.getUnitRegistry().getUnitToRootTransformation(unitConfig), dataFuture);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Transform.class, new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation leading from the root location to the given unit.
     *
     * @param unitConfig the unit where the transformation leads to.
     *
     * @return a transformation future
     */
    public static Future<Transform> getRootToUnitTransformation(final UnitConfig unitConfig) {
        final Future<UnitRegistryData> dataFuture;
        try {
            dataFuture = Registries.getUnitRegistry().getDataFuture();
            return FutureProcessor.allOfInclusiveResultFuture(Registries.getUnitRegistry().getRootToUnitTransformation(unitConfig), dataFuture);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Transform.class, new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method returns the transformation between the given unit A and the given
     * unit B.
     *
     * @param unitConfigA the unit used as transformation base.
     * @param unitConfigB the unit where the transformation leads to.
     *
     * @return a transformation future
     */
    public static Future<Transform> getUnitTransformation(final UnitConfig unitConfigA, final UnitConfig unitConfigB) {
        final Future<UnitRegistryData> dataFuture;
        try {
            dataFuture = Registries.getUnitRegistry().getDataFuture();
            return FutureProcessor.allOfInclusiveResultFuture(Registries.getUnitRegistry().getUnitTransformation(unitConfigA, unitConfigB), dataFuture);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Transform.class, new NotAvailableException("UnitTransformation", ex));
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * label and location scope. The returned unit remote object is fully
     * synchronized with the unit controller and all states locally cached. Use
     * the {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param label         the label to identify the unit.
     * @param locationScope the location scope to identify the unit.
     * @param waitForData   if this flag is set to true the current thread will
     *                      block until the unit remote is fully synchronized with the unit
     *                      controller.
     *
     * @return a new or cached unit remote which can be used to control the unit or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not available
     *                               or the label is not unique enough to identify the unit.
     * @throws InterruptedException  is thrown in case the thread is externally
     *                               interrupted.
     */
    public UnitRemote<?> getUnitByLabelAndLocationScope(final String label, final String locationScope, boolean waitForData) throws NotAvailableException, InterruptedException {
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
                if (ScopeProcessor.generateStringRep(getUnitRegistry().getUnitConfigById(unitConfig.getPlacementConfig().getLocationId()).getScope()).equals(locationScope)) {
                    return getUnit(unitConfig, waitForData);
                }
            }
            throw new InvalidStateException("No unit with Label[" + label + "] found at Location[" + locationScope + "]");

        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Unit[" + label + "]", ex);
        }
    }

    /**
     * Method establishes a connection to the unit referred by the given unit
     * label and location scope. The returned unit remote object is fully
     * synchronized with the unit controller and all states locally cached. Use
     * the {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param label         the label to identify the unit.
     * @param locationScope the location scope to identify the unit.
     * @param waitForData   if this flag is set to true the current thread will
     *                      block until the unit remote is fully synchronized with the unit
     *                      controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     */
    public Future<UnitRemote<?>> getFutureUnitByLabelAndLocationScope(final String label, final String locationScope, boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getUnitByLabelAndLocationScope(label, locationScope, waitForData));
    }

    /**
     * Method establishes a connection to the root location unit.
     * The returned unit remote object is fully synchronized with
     * the unit controller and all states locally cached. Use the
     * {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     *
     * @throws NotAvailableException is thrown in case the unit is not
     *                               available.
     * @throws InterruptedException  is thrown in case the thread is externally
     *                               interrupted
     */
    public static LocationRemote getRootLocation(final boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            return getUnit(Registries.getUnitRegistry().getRootLocationConfig(), waitForData, Units.LOCATION);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("RootLocation", ex);
        }
    }

    /**
     * Method establishes a connection to the root location unit.
     * The returned unit remote object is fully synchronized
     * with the unit controller and all states locally cached. Use
     * the {@code waitForData} flag to block the current thread until the unit
     * remote is fully synchronized with the unit controller during the startup
     * phase. This synchronization is just done once and the current thread will
     * not block if the unit remote was already synchronized before. To force a
     * resynchronization call
     * {@link org.openbase.bco.dal.lib.layer.unit.UnitRemote#requestData()} on the
     * remote instance. Please avoid polling unit states! If you want to get
     * informed about unit config or unit data state changes, please register
     * new config or data observer on this remote instance.
     *
     * @param waitForData if this flag is set to true the current thread will
     *                    block until the unit remote is fully synchronized with the unit
     *                    controller.
     *
     * @return a new or cached unit remote which can be used to control the unit
     * or request all current unit states.
     */
    public Future<LocationRemote> getFutureRootLocation(boolean waitForData) {
        return GlobalCachedExecutorService.submit(() -> getRootLocation(waitForData));
    }
}
