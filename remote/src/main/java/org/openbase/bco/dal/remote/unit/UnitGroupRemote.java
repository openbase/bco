package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import java.util.ArrayList;
import org.openbase.jul.extension.rsb.com.AbstractIdentifiableRemote;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.BrightnessStateServiceRemote;
import org.openbase.bco.dal.remote.service.ColorStateServiceRemote;
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactory;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactoryImpl;
import org.openbase.bco.dal.remote.service.BlindStateServiceRemote;
import org.openbase.bco.dal.remote.service.StandbyStateServiceRemote;
import org.openbase.bco.dal.remote.service.TargetTemperatureStateServiceRemote;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.state.BrightnessStateType.BrightnessState;
import rst.homeautomation.state.ColorStateType.ColorState;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.BlindStateType.BlindState;
import rst.homeautomation.state.StandbyStateType.StandbyState;
import rst.homeautomation.state.TemperatureStateType.TemperatureState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitGroupRemote extends AbstractIdentifiableRemote<UnitGroupConfig> implements BrightnessStateOperationService, ColorStateOperationService, PowerStateOperationService, BlindStateOperationService, StandbyStateOperationService, TargetTemperatureStateOperationService {

    private final Map<ServiceTemplate.ServiceType, AbstractServiceRemote> serviceRemoteMap = new HashMap<>();
    private final ServiceRemoteFactory serviceRemoteFactory;
    private DeviceRegistryRemote deviceRegistryRemote;

    public UnitGroupRemote() throws InstantiationException {
        //TODO: why is the group config used as data type? May we should use a configurable remote instead?
        super(UnitGroupConfig.class);
        serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();
    }

    @Override
    public void notifyDataUpdate(UnitGroupConfig data) throws CouldNotPerformException {
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState brightness) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE);
        return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).setBrightnessState(brightness);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE);
        return ((BrightnessStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE)).getBrightnessState();
    }

    @Override
    public Future<Void> setColorState(ColorState color) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.COLOR_STATE_SERVICE);
        return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).setColorState(color);
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.COLOR_STATE_SERVICE);
        return ((ColorStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.COLOR_STATE_SERVICE)).getColorState();
    }

    @Override
    public Future<Void> setPowerState(PowerState state) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.POWER_STATE_SERVICE);
        return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).setPowerState(state);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.POWER_STATE_SERVICE);
        return ((PowerStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.POWER_STATE_SERVICE)).getPowerState();
    }

    @Override
    public Future<Void> setBlindState(BlindState state) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.BLIND_STATE_SERVICE);
        return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).setBlindState(state);
    }

    @Override
    public BlindState getBlindState() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.BLIND_STATE_SERVICE);
        return ((BlindStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.BLIND_STATE_SERVICE)).getBlindState();
    }

    @Override
    public Future<Void> setStandbyState(StandbyState state) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE);
        return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).setStandbyState(state);
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE);
        return ((StandbyStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.STANDBY_STATE_SERVICE)).getStandbyState();
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState value) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
        return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).setTargetTemperatureState(value);
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
        return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE)).getTargetTemperatureState();
    }

    private void testServiceAvailability(ServiceTemplate.ServiceType serviceType) throws NotAvailableException {
        if (!serviceRemoteMap.containsKey(serviceType)) {
            throw new NotAvailableException("groupConfig." + StringProcessor.transformUpperCaseToCamelCase(serviceType.toString()));
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
            remote.activate();
        }
//        super.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
            remote.deactivate();
        }
//        super.deactivate();
    }

    @Override
    public boolean isActive() {
//        if (!super.isActive()) {
//            return false;
//        }
        for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
            if (!remote.isActive()) {
                return false;
            }
        }
        return true;
    }

    public void init(UnitGroupConfig unitGroupConfig) throws InstantiationException, InitializationException, InterruptedException, CouldNotPerformException {
//        super.init(unitGroupConfig.getScope);

        CachedDeviceRegistryRemote.waitForData();

        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitConfigId : unitGroupConfig.getMemberIdList()) {
            unitConfigs.add(CachedDeviceRegistryRemote.getRegistry().getUnitConfigById(unitConfigId));
        }

        List<UnitConfig> unitConfigsByService = new ArrayList<>();
        for (ServiceTemplate serviceTemplate : unitGroupConfig.getServiceTemplateList()) {
            for (UnitConfig unitConfig : unitConfigs) {
                if (unitHasService(unitConfig, serviceTemplate)) {
                    unitConfigsByService.add(unitConfig);
                }
            }
            // create serviceRemoteByType and unitCOnfiglist
            serviceRemoteMap.put(serviceTemplate.getType(), serviceRemoteFactory.createAndInitServiceRemote(serviceTemplate.getType(), unitConfigsByService));
            unitConfigsByService.clear();
        }
    }

    private boolean unitHasService(UnitConfig unitConfig, ServiceTemplate serviceTemplate) {
        for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            if (serviceConfig.getServiceTemplate().equals(serviceTemplate)) {
                return true;
            }
        }
        return false;
    }
}
