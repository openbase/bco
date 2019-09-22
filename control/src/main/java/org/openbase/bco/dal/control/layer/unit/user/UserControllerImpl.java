package org.openbase.bco.dal.control.layer.unit.user;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.control.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.operation.ActivityMultiStateOperationService;
import org.openbase.bco.dal.remote.action.RemoteActionPool;
import org.openbase.bco.dal.lib.layer.unit.user.UserController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.activity.ActivityConfigType.ActivityConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ActivityMultiStateType.ActivityMultiState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState.State;
import org.openbase.type.domotic.state.UserTransitStateType.UserTransitState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.user.UserDataType.UserData;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.*;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UserControllerImpl extends AbstractBaseUnitController<UserData, UserData.Builder> implements UserController {

    public static final String NET_DEVICE_VARIABLE_IDENTIFIER = "NET_DEVICE";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PresenceState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivityMultiState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserTransitState.getDefaultInstance()));
    }

    private final Object netDeviceDetectorMapLock = new SyncObject("NetDeviceDetectorMapLock");
    private final Map<String, NetDeviceDetector> netDeviceDetectorMap;

    private boolean enabled;

    public UserControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(UserData.newBuilder());
        try {
            this.netDeviceDetectorMap = new HashMap<>();
            registerOperationService(ServiceType.ACTIVITY_MULTI_STATE_SERVICE, new ActivityMultiStateOperationServiceImpl(this));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        MetaConfigVariableProvider variableProvider = new MetaConfigVariableProvider(LabelProcessor.getBestMatch(config.getLabel()), config.getMetaConfig());

        synchronized (netDeviceDetectorMapLock) {

            // shutdown and remove all existing detectors
            for (NetDeviceDetector detector : netDeviceDetectorMap.values()) {
                detector.shutdown();
            }
            netDeviceDetectorMap.clear();
            try {
                for (String netDevice : variableProvider.getValues(NET_DEVICE_VARIABLE_IDENTIFIER).values()) {
                    if (!netDeviceDetectorMap.containsKey(netDevice)) {
                        NetDeviceDetector netDeviceDetector = new NetDeviceDetector();
                        netDeviceDetector.init(netDevice);
                        netDeviceDetectorMap.put(netDevice, netDeviceDetector);
                        netDeviceDetector.addObserver((NetDeviceDetector source, Boolean reachable) -> {
                            synchronized (netDeviceDetectorMapLock) {
                                final PresenceState.Builder presenceState = TimestampProcessor.updateTimestampWithCurrentTime(PresenceState.newBuilder(), logger);
                                for (NetDeviceDetector detector : netDeviceDetectorMap.values()) {
                                    if (detector.isReachable()) {
                                        applyDataUpdate(presenceState.setValue(State.PRESENT).build(), ServiceType.PRESENCE_STATE_SERVICE);
                                        return;
                                    }
                                }
                                applyDataUpdate(presenceState.setValue(State.ABSENT).build(), ServiceType.PRESENCE_STATE_SERVICE);
                            }
                        });
                        if (isActive()) {
                            netDeviceDetector.activate();
                        }
                    }
                }
            } catch (NotAvailableException ex) {
                logger.debug("No net devices found for " + this);
            }
        }
        return super.applyConfigUpdate(config);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        synchronized (netDeviceDetectorMapLock) {
            for (NetDeviceDetector detector : netDeviceDetectorMap.values()) {
                try {
                    detector.activate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not activate  " + detector + "!", ex, logger);
                }
            }
        }
        super.activate();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        synchronized (netDeviceDetectorMapLock) {
            for (NetDeviceDetector detector : netDeviceDetectorMap.values()) {
                try {
                    detector.deactivate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not deactivate " + detector + "!", ex, logger);
                }
            }
        }
        super.deactivate();
    }

    @Override
    protected void applyCustomDataUpdate(UserData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case USER_TRANSIT_STATE_SERVICE:
                updateLastWithCurrentState(PRESENCE_STATE_SERVICE, internalBuilder);

                switch (internalBuilder.getUserTransitState().getValue()) {
                    case LONG_TERM_ABSENT:
                    case SHORT_TERM_ABSENT:
                    case SOON_PRESENT:
                        internalBuilder.getPresenceStateBuilder().setValue(State.ABSENT);
                        break;
                    case LONG_TERM_PRESENT:
                    case SHORT_TERM_PRESENT:
                    case SOON_ABSENT:
                        internalBuilder.getPresenceStateBuilder().setValue(State.PRESENT);
                        break;
                }

                copyResponsibleAction(USER_TRANSIT_STATE_SERVICE, PRESENCE_STATE_SERVICE, internalBuilder);
                updateLocalPosition(internalBuilder);
                break;
            case PRESENCE_STATE_SERVICE:
                updateLocalPosition(internalBuilder);
                break;
            case LOCAL_POSITION_STATE_SERVICE:
                updateLastWithCurrentState(PRESENCE_STATE_SERVICE, internalBuilder);

                if (internalBuilder.getLocalPositionState().getLocationIdCount() == 0) {
                    internalBuilder.getPresenceStateBuilder().setValue(State.ABSENT);
                } else {
                    internalBuilder.getPresenceStateBuilder().setValue(State.PRESENT);
                }

                copyResponsibleAction(LOCAL_POSITION_STATE_SERVICE, PRESENCE_STATE_SERVICE, internalBuilder);
                break;
        }
    }

    private void updateLocalPosition(final UserData.Builder internalBuilder) {
        switch (internalBuilder.getPresenceState().getValue()) {
            case ABSENT:
                updateLastWithCurrentState(LOCAL_POSITION_STATE_SERVICE, internalBuilder);
                internalBuilder.getLocalPositionStateBuilder().clearLocationId().clearPose();
                copyResponsibleAction(PRESENCE_STATE_SERVICE, LOCAL_POSITION_STATE_SERVICE, internalBuilder);
                break;
            case PRESENT:
                if (internalBuilder.getLocalPositionState().getLocationIdCount() == 0) {
                    try {
                        updateLastWithCurrentState(LOCAL_POSITION_STATE_SERVICE, internalBuilder);
                        internalBuilder.getLocalPositionStateBuilder().addLocationId(Registries.getUnitRegistry().getRootLocationConfig().getId());
                        copyResponsibleAction(PRESENCE_STATE_SERVICE, LOCAL_POSITION_STATE_SERVICE, internalBuilder);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not update local position state location id because of user transit update", ex, logger, LogLevel.WARN);
                    }
                }
                break;
        }
    }

    private class NetDeviceDetector extends ObservableImpl<NetDeviceDetector, Boolean> implements Manageable<String> {

        private static final int REACHABLE_TIMEOUT = 5000;
        private static final int REQUEST_PERIOD = 60000;

        private String hostName;
        private Future detectorTask;
        private boolean reachable;

        @Override
        public void init(final String hostName) throws InitializationException, InterruptedException {
            this.hostName = hostName;
        }

        @Override
        public void activate() throws CouldNotPerformException, InterruptedException {
            detectorTask = GlobalScheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    reachable = checkIfReachable();
                    notifyObservers(reachable);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not inform observer about reachable state change!", ex, logger);
                }
            }, 0, REQUEST_PERIOD, TimeUnit.MILLISECONDS);
        }

        @Override
        public void deactivate() throws CouldNotPerformException, InterruptedException {
            detectorTask.cancel(false);
        }

        @Override
        public boolean isActive() {
            return detectorTask != null && !detectorTask.isDone();
        }

        public String getHostName() {
            return hostName;
        }

        public boolean checkIfReachable() {
            try {
                return InetAddress.getByName(hostName).isReachable(REACHABLE_TIMEOUT);
            } catch (IOException ex) {
                return false;
            }
        }

        public boolean isReachable() {
            return reachable;
        }

        @Override
        public void shutdown() {
            try {
                deactivate();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not shutdown " + this, ex, LoggerFactory.getLogger(getClass()));
            }
            super.shutdown();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[host:" + hostName + "]";
        }
    }

    public class ActivityMultiStateOperationServiceImpl implements ActivityMultiStateOperationService {

        private final Map<String, RemoteActionPool> remoteActionPoolMap;
        private UserController userController;

        private final ActionParameter actionParameterPrototype = ActionParameter.newBuilder().setInterruptible(true).setSchedulable(true).setExecutionTimePeriod(Long.MAX_VALUE).build();

        public ActivityMultiStateOperationServiceImpl(final UserController userController) {
            this.userController = userController;
            this.remoteActionPoolMap = new HashMap<>();
        }

        @Override
        public Future<ActionDescription> setActivityMultiState(ActivityMultiState activityMultiState) {
            logger.info("Update activity list[" + activityMultiState.getActivityIdCount() + "]" + this);
            try {
                if (activityMultiState.getActivityIdCount() == 0) {
                    for (Entry<String, RemoteActionPool> stringRemoteActionPoolEntry : remoteActionPoolMap.entrySet()) {
                        stringRemoteActionPoolEntry.getValue().stop();
                    }
                } else if ((activityMultiState.getActivityIdCount() > 0)) {
                    // todo: why is only the first action validated and where are activities disabled and their actions canceled?
                    final String activityId = activityMultiState.getActivityId(0);
                    for (Entry<String, RemoteActionPool> stringRemoteActionPoolEntry : remoteActionPoolMap.entrySet()) {
                        if (!stringRemoteActionPoolEntry.getKey().equals(activityId)) {
                            stringRemoteActionPoolEntry.getValue().stop();
                        }
                    }
                    if (!remoteActionPoolMap.containsKey(activityId)) {
                        final RemoteActionPool remoteActionPool = new RemoteActionPool(UserControllerImpl.this);
                        remoteActionPoolMap.put(activityId, remoteActionPool);
                        final ActivityConfig activityConfig = Registries.getActivityRegistry().getActivityConfigById(activityId);
                        remoteActionPool.initViaServiceStateDescription(activityConfig.getServiceStateDescriptionList(), actionParameterPrototype, () -> true);
                        // todo: check [() -> true)] needs to be implemented. -> true if action is included in current action multi state
                    }
                    remoteActionPoolMap.get(activityId).execute(activityMultiState.getResponsibleAction());
                }

                logger.info("Apply activity data update with {}", activityMultiState.getActivityIdCount());
                applyDataUpdate(activityMultiState.toBuilder().setTimestamp(TimestampProcessor.getCurrentTimestamp()).build(), ServiceType.ACTIVITY_MULTI_STATE_SERVICE);
                return FutureProcessor.completedFuture(null);
            } catch (Exception ex) {
                return FutureProcessor.canceledFuture(new CouldNotPerformException("Could not update activity state of " + this, ex));
            }
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return UserControllerImpl.this;
        }
    }
}
