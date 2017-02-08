package org.openbase.bco.manager.location.core;

/*
 * #%L
 * BCO Manager Location Core
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.openbase.bco.dal.lib.layer.service.provider.ContactStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.layer.unit.connection.Connection;
import org.openbase.bco.dal.remote.service.ServiceRemoteManager;
import org.openbase.bco.manager.location.lib.ConnectionController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.pattern.Observable;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ContactStateType.ContactState;
import rst.domotic.state.DoorStateType.DoorState;
import rst.domotic.state.PassageStateType.PassageState;
import rst.domotic.state.WindowStateType.WindowState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.connection.ConnectionConfigType.ConnectionConfig.ConnectionType;
import rst.domotic.unit.connection.ConnectionDataType.ConnectionData;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionControllerImpl extends AbstractConfigurableController<ConnectionData, ConnectionData.Builder, UnitConfig> implements ConnectionController {

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

        private ContactDoorPosition(DoorState.State correspondingDoorState) {
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

        private ContactWindowPosition(WindowState.State correspondingWindowState) {
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

    public ConnectionControllerImpl() throws InstantiationException {
        super(ConnectionData.newBuilder());
        this.contactDoorPositionMap = new HashMap<>();
        this.contactWindowPositionMap = new HashMap<>();

        this.serviceRemoteManager = new ServiceRemoteManager() {

            @Override
            protected Set<ServiceType> getManagedServiceTypes() throws NotAvailableException, InterruptedException {
                return ConnectionControllerImpl.this.getSupportedServiceTypes();
            }

            @Override
            protected void notifyServiceUpdate(Observable source, Object data) throws NotAvailableException, InterruptedException {
                updateCurrentState();
            }
        };
    }

    private boolean isSupportedServiceType(final ServiceType serviceType) throws CouldNotPerformException, InterruptedException {
        if (serviceType == null) {
            assert false;
            throw new NotAvailableException("ServiceType");
        }
        return getSupportedServiceTypes().contains(serviceType);
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            Registries.getUnitRegistry().waitForData();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        UnitConfig unitConnectionConfig = super.applyConfigUpdate(config);
        serviceRemoteManager.applyConfigUpdate(unitConnectionConfig.getConnectionConfig().getUnitIdList());

        contactDoorPositionMap.clear();
        contactWindowPositionMap.clear();
        for (String unitId : unitConnectionConfig.getConnectionConfig().getUnitIdList()) {
            final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);

            switch (unitConnectionConfig.getConnectionConfig().getType()) {
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

    @Override
    public void registerMethods(RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Connection.class, this, server);
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
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
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update current status!", ex), logger, LogLevel.WARN);
        }
    }

    private void updateDoorState() throws CouldNotPerformException {
        DoorState.State doorState = null;
        try {
            Collection<UnitRemote> contactUnits = serviceRemoteManager.getServiceRemote(ServiceType.CONTACT_STATE_SERVICE).getInternalUnits();
            for (UnitRemote contactStateProvider : contactUnits) {
                ContactState.State contactState = ((ContactStateProviderService) contactStateProvider).getContactState().getValue();
                DoorState.State correspondingDoorState = contactDoorPositionMap.get((String) contactStateProvider.getId()).getCorrespondingDoorState();
                switch (contactState) {
                    case CLOSED:
                        if (doorState == null) {
                            doorState = correspondingDoorState;
                        } else if (doorState != correspondingDoorState) {
                            throw new CouldNotPerformException("Contradicting contact values for the door state!");
                        }
                        break;
                    case OPEN:
                        if (doorState == null) {
                            doorState = DoorState.State.OPEN;
                        }
                        break;
                    case UNKNOWN:
                        logger.warn("Ignoring unknown ConnectionState for DoorState update!");
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

        try (ClosableDataBuilder<ConnectionData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setDoorState(DoorState.newBuilder().setValue(doorState).setTimestamp(Timestamp.newBuilder().setTime(System.currentTimeMillis())));
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply brightness data change!", ex);
        }
    }

    private void updateWindowState() throws CouldNotPerformException {
        WindowState.State windowState = null;
        try {
            Collection<UnitRemote> contactUnits = serviceRemoteManager.getServiceRemote(ServiceType.CONTACT_STATE_SERVICE).getInternalUnits();
            for (UnitRemote contactStateProvider : contactUnits) {
                ContactState.State contactState = ((ContactStateProviderService) contactStateProvider).getContactState().getValue();
                WindowState.State correspondingDoorState = contactWindowPositionMap.get((String) contactStateProvider.getId()).getCorrespondingWindowState();
                switch (contactState) {
                    case CLOSED:
                        if (windowState == null) {
                            windowState = correspondingDoorState;
                        } else if (windowState != correspondingDoorState) {
                            throw new CouldNotPerformException("Contradicting contact values for the window state!");
                        }
                        break;
                    case OPEN:
                        if (windowState == null) {
                            windowState = WindowState.State.OPEN;
                        }
                        break;
                    case UNKNOWN:
                        logger.warn("Ignoring unknown ConnectionState for DoorState update!");
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

        try (ClosableDataBuilder<ConnectionData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setWindowState(WindowState.newBuilder().setValue(windowState).setTimestamp(Timestamp.newBuilder().setTime(System.currentTimeMillis())));
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply brightness data change!", ex);
        }
    }

    private void updatePassageState() throws CouldNotPerformException {
        //TODO: passageState itself has to be designed first
    }

    public ConnectionType getConnectionType() throws NotAvailableException {
        if (!getConfig().getConnectionConfig().hasType()) {
            throw new NotAvailableException("ConnectionConfig.Type");
        }
        return getConfig().getConnectionConfig().getType();
    }

    public void verifyConnectionState(ConnectionType connectionType) throws VerificationFailedException, NotAvailableException {
        if (getConnectionType() != connectionType) {
            throw new VerificationFailedException("ConnectionType verification failed. Connection [" + getConfig().getId() + "] has type [" + getConfig().getConnectionConfig().getType().name() + "] and not [" + connectionType.name() + "]");
        }
    }

    @Override
    public DoorState getDoorState() throws NotAvailableException {
        try {
            verifyConnectionState(ConnectionType.DOOR);
        } catch (VerificationFailedException ex) {
            throw new NotAvailableException("Connection verification shows no door state!", ex);
        }

        try {
            return getData().getDoorState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("DoorState", ex);
        }
    }

    @Override
    public PassageState getPassageState() throws NotAvailableException {
        try {
            verifyConnectionState(ConnectionType.PASSAGE);
        } catch (VerificationFailedException ex) {
            throw new NotAvailableException("Connection verification shows no passage state!", ex);
        }

        try {
            return getData().getPassageState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PassageState", ex);
        }
    }

    @Override
    public WindowState getWindowState() throws NotAvailableException {
        try {
            verifyConnectionState(ConnectionType.WINDOW);
        } catch (VerificationFailedException ex) {
            throw new NotAvailableException("Connection verification shows no window state!", ex);
        }

        try {
            return getData().getWindowState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("WindowState", ex);
        }
    }
}
