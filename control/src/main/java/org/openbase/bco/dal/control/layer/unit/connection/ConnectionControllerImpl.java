package org.openbase.bco.dal.control.layer.unit.connection;

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

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.jp.JPBenchmarkMode;
import org.openbase.bco.dal.lib.layer.service.provider.ContactStateProviderService;
import org.openbase.bco.dal.control.layer.unit.AbstractBaseUnitController;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.layer.unit.connection.ConnectionController;
import org.openbase.bco.dal.remote.layer.service.ServiceRemoteManager;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ContactStateType.ContactState;
import org.openbase.type.domotic.state.DoorStateType.DoorState;
import org.openbase.type.domotic.state.PassageStateType.PassageState;
import org.openbase.type.domotic.state.WindowStateType.WindowState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import org.openbase.type.domotic.unit.connection.ConnectionDataType.ConnectionData;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionControllerImpl extends AbstractBaseUnitController<ConnectionData, ConnectionData.Builder> implements ConnectionController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DoorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PassageState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(WindowState.getDefaultInstance()));
    }

    /**
     * Enumeration describing the relation between the position of a reed contact relative to a door.
     */
    public enum ContactDoorPosition {

        /**
         * If the reed contact is closed than the door is also closed.
         */
        DOOR_CLOSED(DoorState.State.CLOSED),
        /**
         * If the reed contact is closed then the door is open.
         */
        DOOR_OPEN(DoorState.State.OPEN),
        /**
         * If the reed contact is closed then the door is not fully closed nor fully open.
         */
        DOOR_IN_BETWEEN(DoorState.State.IN_BETWEEN);

        private final DoorState.State correspondingDoorState;

        ContactDoorPosition(DoorState.State correspondingDoorState) {
            this.correspondingDoorState = correspondingDoorState;
        }

        public DoorState.State getCorrespondingDoorState() {
            return correspondingDoorState;
        }
    }

    public enum ContactWindowPosition {

        WINDOW_CLOSED(WindowState.State.CLOSED),
        WINDOW_OPEN(WindowState.State.OPEN);

        private final WindowState.State correspondingWindowState;

        ContactWindowPosition(WindowState.State correspondingWindowState) {
            this.correspondingWindowState = correspondingWindowState;
        }

        public WindowState.State getCorrespondingWindowState() {
            return correspondingWindowState;
        }
    }

    public static final ContactDoorPosition DEFAULT_CONTACT_DOOR_POSITION = ContactDoorPosition.DOOR_CLOSED;
    public static final ContactWindowPosition DEFAULT_CONTACT_WINDOW_POSITION = ContactWindowPosition.WINDOW_CLOSED;

    public static final String META_CONFIG_DOOR_POSITION_KEY = "CONTACT_DOOR_POSITION";
    public static final String META_CONFIG_WINDOW_POSITION_KEY = "CONTACT_WINDOW_POSITION";

    private final Map<String, ContactDoorPosition> contactDoorPositionMap;
    private final Map<String, ContactWindowPosition> contactWindowPositionMap;

    private final ServiceRemoteManager serviceRemoteManager;
    private final Set<ServiceType> supportedServiceTypes;

    public ConnectionControllerImpl() throws InstantiationException {
        super(ConnectionData.newBuilder());
        this.contactDoorPositionMap = new HashMap<>();
        this.contactWindowPositionMap = new HashMap<>();
        this.supportedServiceTypes = new HashSet<>();
        this.supportedServiceTypes.add(ServiceType.CONTACT_STATE_SERVICE);

        this.serviceRemoteManager = new ServiceRemoteManager<ConnectionData>(this, getManageLock()) {

            @Override
            protected Set<ServiceType> getManagedServiceTypes() {
                return supportedServiceTypes;
            }

            @Override
            protected void notifyServiceUpdate(final Unit<?> source, Message data) {
                updateCurrentState();
            }
        };
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            Registries.waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
    }

    @Override
    public synchronized UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConnectionConfig = super.applyConfigUpdate(config);
        serviceRemoteManager.applyConfigUpdate(getAggregatedUnitConfigList());

        contactDoorPositionMap.clear();
        contactWindowPositionMap.clear();
        for (String unitId : unitConnectionConfig.getConnectionConfig().getUnitIdList()) {
            final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);

            switch (unitConnectionConfig.getConnectionConfig().getConnectionType()) {
                case DOOR:
                    ContactDoorPosition contactDoorPosition;
                    try {
                        MetaConfigVariableProvider variableProvider = new MetaConfigVariableProvider("doorPositionMetaConfigProvider", unitConfig.getMetaConfig());
                        contactDoorPosition = ContactDoorPosition.valueOf(variableProvider.getValue(META_CONFIG_DOOR_POSITION_KEY));
                    } catch (NotAvailableException | IllegalArgumentException ex) {
                        contactDoorPosition = DEFAULT_CONTACT_DOOR_POSITION;
                    }
                    contactDoorPositionMap.put(unitConfig.getId(), contactDoorPosition);
                    break;
                case WINDOW:
                    ContactWindowPosition contactWindowPosition;
                    try {
                        MetaConfigVariableProvider variableProvider = new MetaConfigVariableProvider("windowPositionVariableProvider", unitConfig.getMetaConfig());
                        contactWindowPosition = ContactWindowPosition.valueOf(variableProvider.getValue(META_CONFIG_WINDOW_POSITION_KEY));
                    } catch (NotAvailableException | IllegalArgumentException ex) {
                        contactWindowPosition = DEFAULT_CONTACT_WINDOW_POSITION;
                    }
                    contactWindowPositionMap.put(unitConfig.getId(), contactWindowPosition);
                    break;
                default:
                    break;
            }
        }

        // if already active than update the current connection state.
        if (isActive()) {
            updateCurrentState();
        }
        return unitConnectionConfig;
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        serviceRemoteManager.activate();
        super.activate();
        updateCurrentState();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        serviceRemoteManager.deactivate();
    }

    private boolean isSupportedServiceType(final ServiceType serviceType) throws CouldNotPerformException, InterruptedException {
        if (serviceType == null) {
            assert false;
            throw new NotAvailableException("ServiceType");
        }
        return this.supportedServiceTypes.contains(serviceType);
    }

    private void updateCurrentState() {
        try {
            switch (getConnectionType()) {
                case DOOR:
                    updateDoorState();
                    break;
                case PASSAGE:
                    updatePassageState();
                    break;
                case WINDOW:
                    updateWindowState();
                    break;
                default:
                    break;
            }
        } catch (CouldNotPerformException ex) {
            if (!JPService.getValue(JPBenchmarkMode.class, false)) {
                ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update current status!", ex), logger, LogLevel.WARN);
            }
        }
    }

    private void updateDoorState() throws CouldNotPerformException {
        DoorState.State doorState = null;
        long timestamp = 0;
        try {
            Collection<UnitRemote> contactUnits = serviceRemoteManager.getServiceRemote(ServiceType.CONTACT_STATE_SERVICE).getInternalUnits();
            for (UnitRemote contactStateProvider : contactUnits) {
                if (!contactStateProvider.isDataAvailable()) {
                    continue;
                }
                ContactState contactState = ((ContactStateProviderService) contactStateProvider).getContactState();
                DoorState.State correspondingDoorState = contactDoorPositionMap.get(contactStateProvider.getId()).getCorrespondingDoorState();
                switch (contactState.getValue()) {
                    case CLOSED:
                        if (doorState == null) {
                            doorState = correspondingDoorState;
                            timestamp = Math.max(timestamp, contactState.getTimestamp().getTime());
                        } else if (doorState != correspondingDoorState) {
                            try {
                                if (!JPService.getProperty(JPBenchmarkMode.class).getValue()) {
                                    throw new CouldNotPerformException("Contradicting contact values for the door state!");
                                }
                            } catch (JPNotAvailableException ex) {
                                // only throw this exception when not in benchmark mode
                            }
                        }
                        break;
                    case OPEN:
                        if (doorState == null) {
                            doorState = DoorState.State.OPEN;
                            timestamp = Math.max(timestamp, contactState.getTimestamp().getTime());
                        }
                        break;
                    case UNKNOWN:
                        logger.debug("Ignoring unknown ConnectionState for DoorState update!");
                        break;
                    default:
                        break;
                }
            }
            if (doorState == null) {
                return;
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update door state", ex);
        }

        try {
            applyDataUpdate(TimestampProcessor.updateTimestamp(timestamp, DoorState.newBuilder().setValue(doorState), TimeUnit.MICROSECONDS, logger), ServiceType.DOOR_STATE_SERVICE);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply new door state!", ex);
        }
    }

    private void updateWindowState() throws CouldNotPerformException {
        WindowState.State windowState = null;
        long timestamp = 0;
        try {
            Collection<UnitRemote> contactUnits = serviceRemoteManager.getServiceRemote(ServiceType.CONTACT_STATE_SERVICE).getInternalUnits();
            for (UnitRemote contactStateProvider : contactUnits) {
                if (!contactStateProvider.isDataAvailable()) {
                    continue;
                }
                ContactState contactState = ((ContactStateProviderService) contactStateProvider).getContactState();
                WindowState.State correspondingWindowState = contactWindowPositionMap.get(contactStateProvider.getId()).getCorrespondingWindowState();
                switch (contactState.getValue()) {
                    case CLOSED:
                        if (windowState == null) {
                            windowState = correspondingWindowState;
                            timestamp = Math.max(timestamp, contactState.getTimestamp().getTime());
                        } else if (windowState != correspondingWindowState) {
                            try {
                                if (!JPService.getProperty(JPBenchmarkMode.class).getValue()) {
                                    throw new CouldNotPerformException("Contradicting contact values for the window state!");
                                }
                            } catch (JPNotAvailableException ex) {
                                // only throw this exception when not in benchmark mode
                            }
                        }
                        break;
                    case OPEN:
                        if (windowState == null) {
                            windowState = WindowState.State.OPEN;
                            timestamp = Math.max(timestamp, contactState.getTimestamp().getTime());
                        }
                        break;
                    case UNKNOWN:
                        logger.debug("Ignoring unknown ConnectionState for DoorState update!");
                        break;
                    default:
                        break;
                }
            }
            if (windowState == null) {
                return;
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update door state", ex);
        }

        try {
            applyDataUpdate(TimestampProcessor.updateTimestamp(timestamp, WindowState.newBuilder().setValue(windowState), TimeUnit.MICROSECONDS, logger), ServiceType.WINDOW_STATE_SERVICE);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply new window state!", ex);
        }
    }

    private void updatePassageState() throws CouldNotPerformException {
        // passage does not offer any states to update.
    }

    public ConnectionType getConnectionType() throws NotAvailableException {
        if (!getConfig().getConnectionConfig().hasConnectionType()) {
            throw new NotAvailableException("ConnectionConfig.Type");
        }
        return getConfig().getConnectionConfig().getConnectionType();
    }

    protected List<UnitConfig> getAggregatedUnitConfigList() throws NotAvailableException {
        final ArrayList<UnitConfig> unitConfigList = new ArrayList<>();

        // init service unit list
        for (final String unitId : getConfig().getConnectionConfig().getUnitIdList()) {
            // resolve unit config by unit registry
            UnitConfig unitConfig;
            try {
                unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
            } catch (NotAvailableException ex) {
                logger.warn("Unit[" + unitId + "] not available for [" + this + "]");
                continue;
            }

            // filter disabled units
            if (!UnitConfigProcessor.isEnabled(unitConfig)) {
                continue;
            }

            unitConfigList.add(unitConfig);
        }
        return unitConfigList;
    }
}
