/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.core;

/*
 * #%L
 * COMA LocationManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dc.bco.dal.remote.service.AbstractServiceRemote;
import org.dc.bco.dal.remote.service.HandleProviderRemote;
import org.dc.bco.dal.remote.service.ReedSwitchProviderRemote;
import org.dc.bco.dal.remote.service.ServiceRemoteFactoryImpl;
import org.dc.bco.manager.location.lib.Connection;
import org.dc.bco.manager.location.lib.ConnectionController;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.com.AbstractConfigurableController;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.HandleStateType;
import rst.homeautomation.state.ReedSwitchStateType;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.ConnectionDataType.ConnectionData;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ConnectionControllerImpl extends AbstractConfigurableController<ConnectionData, ConnectionData.Builder, ConnectionConfig> implements ConnectionController {

    private final Map<ServiceType, AbstractServiceRemote> serviceRemoteMap;

    public ConnectionControllerImpl(ConnectionConfig connection) throws InstantiationException {
        super(ConnectionData.newBuilder());
        serviceRemoteMap = new HashMap<>();
    }

    @Override
    public void init(final ConnectionConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            Map<ServiceType, List<UnitConfigType.UnitConfig>> unitConfigByServiceMap = new HashMap<>();
            for (ServiceType serviceType : ServiceType.values()) {
                unitConfigByServiceMap.put(serviceType, new ArrayList<>());
            }
            DeviceRegistry deviceRegistry = LocationManagerController.getInstance().getDeviceRegistry();
            for (UnitConfigType.UnitConfig unitConfig : deviceRegistry.getUnitConfigs()) {
                if (config.getUnitIdList().contains(unitConfig.getId())) {
                    for (ServiceType serviceType : deviceRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTypeList()) {
                        unitConfigByServiceMap.get(serviceType).add(unitConfig);
                    }
                }
            }
            for (Map.Entry<ServiceType, List<UnitConfigType.UnitConfig>> entry : unitConfigByServiceMap.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                serviceRemoteMap.put(entry.getKey(), ServiceRemoteFactoryImpl.getInstance().createAndInitServiceRemote(entry.getKey(), entry.getValue()));
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void registerMethods(RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Connection.class, this, server);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
            remote.activate();
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
            remote.deactivate();
        }
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
    }

    @Override
    public HandleStateType.HandleState getHandle() throws CouldNotPerformException {
        try {
            return ((HandleProviderRemote) serviceRemoteMap.get(ServiceType.HANDLE_PROVIDER)).getHandle();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("handleProviderRemote");
        }
    }

    @Override
    public ReedSwitchStateType.ReedSwitchState getReedSwitch() throws CouldNotPerformException {
        try {
            return ((ReedSwitchProviderRemote) serviceRemoteMap.get(ServiceType.REED_SWITCH_PROVIDER)).getReedSwitch();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("reedSwitchProviderRemote");
        }
    }
}
