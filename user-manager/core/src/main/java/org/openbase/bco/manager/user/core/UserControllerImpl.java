package org.openbase.bco.manager.user.core;

/*
 * #%L
 * BCO Manager User Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.manager.user.lib.UserController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivityMultiStateType.ActivityMultiState;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.PresenceStateType.PresenceState.State;
import rst.domotic.state.UserTransitStateType.UserTransitState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.user.UserDataType.UserData;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.*;

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
        super(UserControllerImpl.class, UserData.newBuilder());
        this.netDeviceDetectorMap = new HashMap<>();
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
                        netDeviceDetector.addObserver((Observable<Boolean> source, Boolean reachable) -> {
                            synchronized (netDeviceDetectorMapLock) {
                                for (NetDeviceDetector detector : netDeviceDetectorMap.values()) {
                                    if (detector.isReachable()) {
                                        setPresenceState(PresenceState.newBuilder().setValue(State.PRESENT).build());
                                        return;
                                    }
                                }
                                setPresenceState(PresenceState.newBuilder().setValue(State.ABSENT).build());
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
    public void enable() throws CouldNotPerformException, InterruptedException {
        enabled = true;
        activate();
    }

    @Override
    public void disable() throws CouldNotPerformException, InterruptedException {
        enabled = false;
        deactivate();
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
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Future<ActionFuture> setActivityMultiState(final ActivityMultiState activityMultiState) throws CouldNotPerformException {
        return applyUnauthorizedAction(ActionDescriptionProcessor.generateActionDescriptionBuilderAndUpdate(activityMultiState, ACTIVITY_MULTI_STATE_SERVICE,this).build());
    }

    @Override
    public Future<ActionFuture> setPresenceState(PresenceState presenceState) throws CouldNotPerformException {
        return applyUnauthorizedAction(ActionDescriptionProcessor.generateActionDescriptionBuilderAndUpdate(presenceState, PRESENCE_STATE_SERVICE,this).build());
    }

    @Override
    public Future<ActionFuture> setUserTransitState(UserTransitState userTransitState) throws CouldNotPerformException {
        applyDataUpdate(userTransitState, USER_TRANSIT_STATE_SERVICE);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected void applyCustomDataUpdate(UserData.Builder internalBuilder, ServiceType serviceType) {
        logger.info("state;" + internalBuilder.build());
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

                break;
        }
    }

    private class NetDeviceDetector extends ObservableImpl<Boolean> implements Manageable<String> {

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
                ExceptionPrinter.printHistory(new NotAvailableException(hostName + " is not reachable!", ex), logger);
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
}
