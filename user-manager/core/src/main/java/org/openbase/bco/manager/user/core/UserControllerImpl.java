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
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.manager.user.lib.UserController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
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
import rst.domotic.state.UserActivityStateType.UserActivityState;
import rst.domotic.state.UserPresenceStateType.UserPresenceState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.user.UserDataType.UserData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class UserControllerImpl extends AbstractBaseUnitController<UserData, UserData.Builder> implements UserController {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
    }
    
    public static final String NET_DEVICE_VARIABLE_IDENTIFIER = "NET_DEVICE";
    
    private boolean enabled;
    private final Object netDeviceDetectorMapLock = new SyncObject("NetDeviceDetectorMapLock");
    
    private final Map<String, NetDeviceDetector> netDeviceDetectorMap;
    
    public UserControllerImpl() throws org.openbase.jul.exception.InstantiationException {
        super(UserControllerImpl.class, UserData.newBuilder());
        this.netDeviceDetectorMap = new HashMap<>();
    }
    
    @Override
    public UnitConfig applyConfigUpdate(UnitConfig config) throws CouldNotPerformException, InterruptedException {
        setDataField(TYPE_FIELD_USER_NAME, config.getUserConfig().getUserName());
        MetaConfigVariableProvider variableProvider = new MetaConfigVariableProvider(config.getLabel(), config.getMetaConfig());
        
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
                                        setUserPresenceState(UserPresenceState.newBuilder().setValue(UserPresenceState.State.AT_HOME).build());
                                        return;
                                    }
                                }
                                setUserPresenceState(UserPresenceState.newBuilder().setValue(UserPresenceState.State.AWAY).build());
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
    public String getUserName() throws NotAvailableException {
        try {
            return getConfig().getUserConfig().getUserName();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("username", ex);
        }
    }
    
    @Override
    public UserActivityState getUserActivityState() throws NotAvailableException {
        try {
            return getData().getUserActivityState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user activity", ex);
        }
    }
    
    @Override
    public UserPresenceState getUserPresenceState() throws NotAvailableException {
        try {
            return getData().getUserPresenceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("user presence state", ex);
        }
    }
    
    @Override
    public Future<ActionFuture> setUserActivityState(UserActivityState UserActivityState) throws CouldNotPerformException {
        try (ClosableDataBuilder<UserData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setUserActivityState(UserActivityState);
        } catch (CouldNotPerformException | NullPointerException ex) {
            throw new CouldNotPerformException("Could not set user activity to [" + UserActivityState + "] for " + this + "!", ex);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public Future<ActionFuture> setUserPresenceState(UserPresenceState userPresenceState) throws CouldNotPerformException {
        try (ClosableDataBuilder<UserData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setUserPresenceState(userPresenceState);
        } catch (CouldNotPerformException | NullPointerException ex) {
            throw new CouldNotPerformException("Could not set user presence state to [" + userPresenceState + "] for " + this + "!", ex);
        }
        return CompletableFuture.completedFuture(null);
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
