package org.openbase.bco.manager.location.core;

/*
 * #%L
 * COMA LocationManager Core
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.provider.ContactStateProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.HandleStateProviderService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactory;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.openbase.bco.manager.location.lib.Connection;
import org.openbase.bco.manager.location.lib.ConnectionController;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
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
import org.openbase.jul.pattern.Observer;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
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

    public static final ContactDoorPosition DEFAULT_CONTACT_DOOR_POSITION = ContactDoorPosition.DOOR_CLOSED;

    private final UnitRemoteFactory factory;
    private final Map<String, UnitRemote> unitRemoteMap;
    private final Map<ServiceType, Collection<? extends Service>> serviceMap;
    private List<String> originalUnitIdList;
    private UnitRegistry unitRegistry;

    private MetaConfigVariableProvider metaConfigContactDoorPositionProvider;
    private final Map<String, ContactDoorPosition> contactDoorPositionMap;

    public ConnectionControllerImpl() throws InstantiationException {
        super(ConnectionData.newBuilder());
        this.factory = UnitRemoteFactoryImpl.getInstance();
        this.unitRemoteMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
        this.contactDoorPositionMap = new HashMap<>();
    }

    private boolean isSupportedServiceType(final ServiceType serviceType) {
        switch (serviceType) {
            case HANDLE_STATE_SERVICE:
            case CONTACT_STATE_SERVICE:
                return true;
            default:
                return false;
        }
    }

    private boolean isSupportedServiceType(final List<ServiceTemplate> serviceTemplates) {
        return serviceTemplates.stream().anyMatch((serviceTemplate) -> (isSupportedServiceType(serviceTemplate.getType())));
    }

    private void addRemoteToServiceMap(final ServiceType serviceType, final UnitRemote unitRemote) {
        //TODO: should be replaced with generic class loading
        // and the update can be realized with reflections or the setField method and a notify
        switch (serviceType) {
            case HANDLE_STATE_SERVICE:
                ((ArrayList<HandleStateProviderService>) serviceMap.get(ServiceType.HANDLE_STATE_SERVICE)).add((HandleStateProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        updateCurrentStatus();
//                        HandleState handle = getHandleState();
                        // data has to be fused to build DOOR/WINDOW or PASSAGE States
                    }
                });
                break;
            case CONTACT_STATE_SERVICE:
                ((ArrayList<ContactStateProviderService>) serviceMap.get(ServiceType.CONTACT_STATE_SERVICE)).add((ContactStateProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        updateCurrentStatus();
                    }
                });

                try {
                    if (getConfig().getConnectionConfig().getType() == ConnectionType.DOOR) {
                        ContactDoorPosition contactDoorPosition;
                        try {
                            metaConfigContactDoorPositionProvider = new MetaConfigVariableProvider("doorPositionMetaConfigProvider", ((UnitConfig) unitRemote.getConfig()).getMetaConfig());
                            contactDoorPosition = ContactDoorPosition.valueOf(metaConfigContactDoorPositionProvider.getValue("CONTACT_DOOR_POSITION"));
                        } catch (NotAvailableException | IllegalArgumentException ex) {
                            contactDoorPosition = DEFAULT_CONTACT_DOOR_POSITION;
                        }
                        contactDoorPositionMap.put((String) unitRemote.getId(), contactDoorPosition);
                    }
                } catch (NotAvailableException ex) {
                    ExceptionPrinter.printHistory("Id for unit with contactState not available!", ex, logger);
                }
                break;
        }
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            CachedUnitRegistryRemote.waitForData();
            unitRegistry = CachedUnitRegistryRemote.getRegistry();
            originalUnitIdList = config.getConnectionConfig().getUnitIdList();
            for (ServiceType serviceType : ServiceType.values()) {
                if (isSupportedServiceType(serviceType)) {
                    serviceMap.put(serviceType, new ArrayList<>());
                }
            }
            for (UnitConfig unitConfig : unitRegistry.getUnitConfigs()) {
                if (config.getConnectionConfig().getUnitIdList().contains(unitConfig.getId())) {
                    List<ServiceTemplate> serviceTemplate = unitRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTemplateList();

                    // ignore units that do not have any service supported by a location
                    if (!isSupportedServiceType(serviceTemplate)) {
                        continue;
                    }

                    UnitRemote unitRemote = factory.newInitializedInstance(unitConfig);
                    unitRemoteMap.put(unitConfig.getId(), unitRemote);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        super.init(config);
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        List<String> newUnitIdList = new ArrayList<>(config.getConnectionConfig().getUnitIdList());
        for (String originalId : originalUnitIdList) {
            if (config.getConnectionConfig().getUnitIdList().contains(originalId)) {
                newUnitIdList.remove(originalId);
            } else {
                unitRemoteMap.get(originalId).deactivate();
                unitRemoteMap.remove(originalId);
                for (Collection<? extends Service> serviceCollection : serviceMap.values()) {
                    for (Service service : new ArrayList<>(serviceCollection)) {
                        if (((UnitRemote) service).getId().equals(originalId)) {
                            serviceCollection.remove(service);
                        }
                    }
                }
            }
        }
        for (String newUnitId : newUnitIdList) {
            final UnitConfig unitConfig = unitRegistry.getUnitConfigById(newUnitId);
            List<ServiceTemplate> serviceTemplates = new ArrayList<>();

            // ignore units that do not have any service supported by a location
            if (!isSupportedServiceType(serviceTemplates)) {
                continue;
            }

            UnitRemote unitRemote = factory.newInitializedInstance(unitConfig);
            unitRemoteMap.put(unitConfig.getId(), unitRemote);
            if (isActive()) {
                for (ServiceTemplate serviceTempaltes : serviceTemplates) {
                    addRemoteToServiceMap(serviceTempaltes.getType(), unitRemote);
                }
                unitRemote.activate();
            }
        }
        if (isActive()) {
            updateCurrentStatus();
        }
        originalUnitIdList = config.getConnectionConfig().getUnitIdList();
        return super.applyConfigUpdate(config);
    }

    @Override
    public void registerMethods(RSBLocalServer server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Connection.class, this, server);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            unitRemote.activate();
        }
        super.activate();

        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            for (ServiceTemplate serviceTemplate : unitRegistry.getUnitTemplateByType(unitRemote.getType()).getServiceTemplateList()) {
                addRemoteToServiceMap(serviceTemplate.getType(), unitRemote);
            }
        }
        updateCurrentStatus();
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            unitRemote.deactivate();
        }
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
    }

    private void updateCurrentStatus() {
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

    private Collection<UnitRemote> getContactStateProviderServices() {
        return (Collection<UnitRemote>) serviceMap.get(ServiceType.CONTACT_STATE_SERVICE);
    }

    private void updateDoorState() throws CouldNotPerformException {
        DoorState.State doorState = null;
        try {
            for (UnitRemote contactStateProvider : getContactStateProviderServices()) {
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
                        doorState = DoorState.State.OPEN;
                        break;
                    case UNKNOWN:
                        logger.warn("Ignoring unknown ConnectionState for DoorState update!");
                        break;
                    default:
                        break;
                }
                if (contactState == ContactState.State.CLOSED) {
                    if (doorState == null) {
                        doorState = contactDoorPositionMap.get((String) contactStateProvider.getId()).getCorrespondingDoorState();
                    } else if (doorState != contactDoorPositionMap.get((String) contactStateProvider.getId()).getCorrespondingDoorState()) {
                        throw new CouldNotPerformException("Contradicting contact values for the door state!");
                    }
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

    }

    private void updatePassageState() throws CouldNotPerformException {

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
