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
import org.openbase.bco.dal.lib.layer.service.provider.HandleProviderService;
import org.openbase.bco.dal.lib.layer.service.provider.ReedSwitchProviderService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactory;
import org.openbase.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.openbase.bco.manager.location.lib.Connection;
import org.openbase.bco.manager.location.lib.ConnectionController;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableController;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.service.ServiceTemplateType;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.HandleStateType;
import rst.homeautomation.state.ReedSwitchStateType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.ConnectionDataType.ConnectionData;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionControllerImpl extends AbstractConfigurableController<ConnectionData, ConnectionData.Builder, ConnectionConfig> implements ConnectionController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ConnectionData.getDefaultInstance()));
    }

    private final UnitRemoteFactory factory;
    private final Map<String, UnitRemote> unitRemoteMap;
    private final Map<ServiceTemplateType.ServiceTemplate.ServiceType, Collection<? extends Service>> serviceMap;
    private List<String> originalUnitIdList;

    public ConnectionControllerImpl(ConnectionConfig connection) throws InstantiationException {
        super(ConnectionData.newBuilder());
        this.factory = UnitRemoteFactoryImpl.getInstance();
        this.unitRemoteMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
    }

    private boolean isSupportedServiceType(final ServiceType serviceType) {
        switch (serviceType) {
            case HANDLE_PROVIDER:
            case REED_SWITCH_PROVIDER:
                return true;
            default:
                return false;
        }
    }

    private boolean isSupportedServiceType(final List<ServiceType> serviceTypes) {
        return serviceTypes.stream().anyMatch((serviceType) -> (isSupportedServiceType(serviceType)));
    }

    private void addRemoteToServiceMap(final ServiceType serviceType, final UnitRemote unitRemote) {
        //TODO: should be replaced with generic class loading
        // and the update can be realized with reflections or the setField method and a notify
        switch (serviceType) {
            case HANDLE_PROVIDER:
                ((ArrayList<HandleProviderService>) serviceMap.get(ServiceType.HANDLE_PROVIDER)).add((HandleProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        HandleStateType.HandleState handle = getHandle();
                        try (ClosableDataBuilder<ConnectionData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setHandleState(handle);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply data change!", ex);
                        }
                    }
                });
                break;
            case REED_SWITCH_PROVIDER:
                ((ArrayList<ReedSwitchProviderService>) serviceMap.get(ServiceType.REED_SWITCH_PROVIDER)).add((ReedSwitchProviderService) unitRemote);
                unitRemote.addDataObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        ReedSwitchStateType.ReedSwitchState reedSwitch = getReedSwitch();
                        try (ClosableDataBuilder<ConnectionData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setReedSwitchState(reedSwitch);
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply data change!", ex);
                        }
                    }
                });
                break;
        }
    }

    @Override
    public void init(final ConnectionConfig config) throws InitializationException, InterruptedException {
        try {
            originalUnitIdList = config.getUnitIdList();
            for (ServiceType serviceType : ServiceType.values()) {
                if (isSupportedServiceType(serviceType)) {
                    serviceMap.put(serviceType, new ArrayList<>());
                }
            }
            DeviceRegistry deviceRegistry = LocationManagerController.getInstance().getDeviceRegistry();
            for (UnitConfigType.UnitConfig unitConfig : deviceRegistry.getUnitConfigs()) {
                if (config.getUnitIdList().contains(unitConfig.getId())) {
                    List<ServiceType> serviceTypes = deviceRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTypeList();

                    // ignore units that do not have any service supported by a location
                    if (!isSupportedServiceType(serviceTypes)) {
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
    public ConnectionConfig applyConfigUpdate(final ConnectionConfig config) throws CouldNotPerformException, InterruptedException {
        List<String> newUnitIdList = new ArrayList<>(config.getUnitIdList());
        for (String originalId : originalUnitIdList) {
            if (config.getUnitIdList().contains(originalId)) {
                newUnitIdList.remove(originalId);
            } else {
                unitRemoteMap.get(originalId).deactivate();
                unitRemoteMap.remove(originalId);
                for (Collection<? extends Service> serviceCollection : serviceMap.values()) {
                    Collection serviceSelectionCopy = new ArrayList<>(serviceCollection);
                    for (Object service : serviceSelectionCopy) {
                        if (((UnitRemote) service).getId().equals(originalId)) {
                            serviceCollection.remove(service);
                        }
                    }
                }
            }
        }
        for (String newUnitId : newUnitIdList) {
            DeviceRegistry deviceRegistry = LocationManagerController.getInstance().getDeviceRegistry();
            UnitConfig unitConfig = deviceRegistry.getUnitConfigById(newUnitId);
            List<ServiceType> serviceTypes = new ArrayList<>();

            // ignore units that do not have any service supported by a location
            if (!isSupportedServiceType(serviceTypes)) {
                continue;
            }

            UnitRemote unitRemote = factory.newInitializedInstance(unitConfig);
            unitRemoteMap.put(unitConfig.getId(), unitRemote);
            if (isActive()) {
                for (ServiceType serviceType : serviceTypes) {
                    addRemoteToServiceMap(serviceType, unitRemote);
                }
                unitRemote.activate();
            }
        }
        if (isActive()) {
            getCurrentStatus();
        }
        originalUnitIdList = config.getUnitIdList();
        return super.applyConfigUpdate(config);
    }

    @Override
    public void registerMethods(RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Connection.class, this, server);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            unitRemote.activate();
        }
        super.activate();

        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            for (ServiceType serviceType : LocationManagerController.getInstance().getDeviceRegistry().getUnitTemplateByType(unitRemote.getType()).getServiceTypeList()) {
                addRemoteToServiceMap(serviceType, unitRemote);
            }
        }
        getCurrentStatus();
    }

    private void getCurrentStatus() {
        try {
            HandleStateType.HandleState handle = getHandle();
            ReedSwitchStateType.ReedSwitchState reedSwitch = getReedSwitch();
            try (ClosableDataBuilder<ConnectionData.Builder> dataBuilder = getDataBuilder(this)) {
                dataBuilder.getInternalBuilder().setHandleState(handle);
                dataBuilder.getInternalBuilder().setReedSwitchState(reedSwitch);
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not apply data change!", ex);
            }
        } catch (CouldNotPerformException ex) {
            logger.warn("Could not get current status", ex);
        }
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

    @Override
    public Collection<HandleProviderService> getHandleStateProviderServices() {
        return (Collection<HandleProviderService>) serviceMap.get(ServiceType.HANDLE_PROVIDER);
    }

    @Override
    public Collection<ReedSwitchProviderService> getReedSwitchStateProviderServices() {
        return (Collection<ReedSwitchProviderService>) serviceMap.get(ServiceType.REED_SWITCH_PROVIDER);
    }
}
