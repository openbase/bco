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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.BrightnessStateType.BrightnessState;
import rst.homeautomation.state.ColorStateType.ColorState;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.BlindStateType.BlindState;
import rst.homeautomation.state.StandbyStateType.StandbyState;
import rst.homeautomation.state.TemperatureStateType.TemperatureState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupRemote extends AbstractIdentifiableRemote<UnitConfig> implements BrightnessStateOperationService, ColorStateOperationService, PowerStateOperationService, BlindStateOperationService, StandbyStateOperationService, TargetTemperatureStateOperationService {

    private final Map<ServiceTemplate, AbstractServiceRemote> serviceRemoteMap = new HashMap<>();
    private final ServiceRemoteFactory serviceRemoteFactory;

    public UnitGroupRemote() throws InstantiationException {
        //TODO: why is the group config used as data type? May we should use a configurable remote instead?
        super(UnitConfig.class);
        serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();
    }

    @Override
    public void notifyDataUpdate(UnitConfig data) throws CouldNotPerformException {
    }

    @Override
    public Future<Void> setBrightnessState(BrightnessState brightness) throws CouldNotPerformException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.BRIGHTNESS_STATE_SERVICE).setPattern(ServicePattern.OPERATION).build();
        testServiceAvailability(serviceTemplate);
        return ((BrightnessStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).setBrightnessState(brightness);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.BRIGHTNESS_STATE_SERVICE).setPattern(ServicePattern.PROVIDER).build();
        testServiceAvailability(serviceTemplate);
        return ((BrightnessStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).getBrightnessState();
    }

    @Override
    public Future<Void> setColorState(ColorState color) throws CouldNotPerformException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.COLOR_STATE_SERVICE).setPattern(ServicePattern.OPERATION).build();
        testServiceAvailability(serviceTemplate);
        return ((ColorStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).setColorState(color);
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.COLOR_STATE_SERVICE).setPattern(ServicePattern.PROVIDER).build();
        testServiceAvailability(serviceTemplate);
        return ((ColorStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).getColorState();
    }

    @Override
    public Future<Void> setPowerState(PowerState state) throws CouldNotPerformException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.POWER_STATE_SERVICE).setPattern(ServicePattern.OPERATION).build();
        testServiceAvailability(serviceTemplate);
        return ((PowerStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).setPowerState(state);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.POWER_STATE_SERVICE).setPattern(ServicePattern.PROVIDER).build();
        testServiceAvailability(serviceTemplate);
        return ((PowerStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).getPowerState();
    }

    @Override
    public Future<Void> setBlindState(BlindState state) throws CouldNotPerformException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.BLIND_STATE_SERVICE).setPattern(ServicePattern.OPERATION).build();
        testServiceAvailability(serviceTemplate);
        return ((BlindStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).setBlindState(state);
    }

    @Override
    public BlindState getBlindState() throws NotAvailableException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.BLIND_STATE_SERVICE).setPattern(ServicePattern.PROVIDER).build();
        testServiceAvailability(serviceTemplate);
        return ((BlindStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).getBlindState();
    }

    @Override
    public Future<Void> setStandbyState(StandbyState state) throws CouldNotPerformException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.STANDBY_STATE_SERVICE).setPattern(ServicePattern.OPERATION).build();
        testServiceAvailability(serviceTemplate);
        return ((StandbyStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).setStandbyState(state);
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.STANDBY_STATE_SERVICE).setPattern(ServicePattern.PROVIDER).build();
        testServiceAvailability(serviceTemplate);
        return ((StandbyStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).getStandbyState();
    }

    @Override
    public Future<Void> setTargetTemperatureState(TemperatureState value) throws CouldNotPerformException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE).setPattern(ServicePattern.OPERATION).build();
        testServiceAvailability(serviceTemplate);
        return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).setTargetTemperatureState(value);
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        ServiceTemplate serviceTemplate = ServiceTemplate.newBuilder().setType(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE).setPattern(ServicePattern.PROVIDER).build();
        testServiceAvailability(serviceTemplate);
        return ((TargetTemperatureStateServiceRemote) serviceRemoteMap.get(serviceTemplate)).getTargetTemperatureState();
    }

    private void testServiceAvailability(ServiceTemplate serviceTemplate) throws NotAvailableException {
        if (!serviceRemoteMap.containsKey(serviceTemplate)) {
            throw new NotAvailableException("groupConfig." + StringProcessor.transformUpperCaseToCamelCase(serviceTemplate.toString()));
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
            remote.activate();
        }
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
            remote.deactivate();
        }
    }

    @Override
    public boolean isActive() {
        return serviceRemoteMap.values().stream().noneMatch((remote) -> (!remote.isActive()));
    }

    public void init(UnitConfig unitGroupConfig) throws InstantiationException, InitializationException, InterruptedException, CouldNotPerformException {
        CachedDeviceRegistryRemote.waitForData();

        List<UnitConfig> unitConfigs = new ArrayList<>();
        for (String unitConfigId : unitGroupConfig.getUnitGroupConfig().getMemberIdList()) {
            unitConfigs.add(CachedDeviceRegistryRemote.getRegistry().getUnitConfigById(unitConfigId));
        }

        List<UnitConfig> unitConfigsByService = new ArrayList<>();
        for (ServiceTemplate serviceTemplate : unitGroupConfig.getUnitGroupConfig().getServiceTemplateList()) {
            unitConfigs.stream().filter((unitConfig) -> (unitHasService(unitConfig, serviceTemplate))).forEach((unitConfig) -> {
                unitConfigsByService.add(unitConfig);
            });
            // create serviceRemoteByType and unitCOnfiglist
            serviceRemoteMap.put(serviceTemplate, serviceRemoteFactory.createAndInitServiceRemote(serviceTemplate.getType(), unitConfigsByService));
            unitConfigsByService.clear();
        }
    }

    private boolean unitHasService(UnitConfig unitConfig, ServiceTemplate serviceTemplate) {
        return unitConfig.getServiceConfigList().stream().anyMatch((serviceConfig) -> (serviceConfig.getServiceTemplate().equals(serviceTemplate)));
    }
}
