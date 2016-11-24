package org.openbase.bco.dal.remote.unit.unitgroup;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.unit.unitgroup.UnitGroup;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.BlindStateServiceRemote;
import org.openbase.bco.dal.remote.service.BrightnessStateServiceRemote;
import org.openbase.bco.dal.remote.service.ColorStateServiceRemote;
import org.openbase.bco.dal.remote.service.PowerStateServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactory;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactoryImpl;
import org.openbase.bco.dal.remote.service.StandbyStateServiceRemote;
import org.openbase.bco.dal.remote.service.TargetTemperatureStateServiceRemote;
import org.openbase.bco.dal.remote.unit.AbstractUnitRemote;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.unitgroup.UnitGroupDataType.UnitGroupData;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupRemote extends AbstractUnitRemote<UnitGroupData> implements UnitGroup {

    private final Map<ServiceTemplate, AbstractServiceRemote> serviceRemoteMap = new HashMap<>();
    private final ServiceRemoteFactory serviceRemoteFactory;

    public UnitGroupRemote() throws InstantiationException {
        super(UnitGroupData.class);
        serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();
        // todo: reimplement as real remote with manager.
    }

    @Override
    public void init(final UnitConfig unitGroupUnitConfig) throws InitializationException, InterruptedException {
        try {
            CachedDeviceRegistryRemote.waitForData();

            if (!unitGroupUnitConfig.hasUnitGroupConfig()) {
                throw new VerificationFailedException("Given unit config does not contain a unit group config!");
            }

            if (unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList().isEmpty()) {
                throw new VerificationFailedException("UnitGroupConfig has no unit members!");
            }

            List<UnitConfig> unitConfigs = new ArrayList<>();
            for (String unitConfigId : unitGroupUnitConfig.getUnitGroupConfig().getMemberIdList()) {
                unitConfigs.add(CachedDeviceRegistryRemote.getRegistry().getUnitConfigById(unitConfigId));
            }

            if (unitConfigs.isEmpty()) {
                throw new CouldNotPerformException("Could not resolve any unit members!");
            }

            List<UnitConfig> unitConfigsByService = new ArrayList<>();
            for (ServiceTemplate serviceTemplate : unitGroupUnitConfig.getUnitGroupConfig().getServiceTemplateList()) {
                unitConfigs.stream().filter((unitConfig) -> (unitHasService(unitConfig, serviceTemplate))).forEach((unitConfig) -> {
                    unitConfigsByService.add(unitConfig);
                });
                // create serviceRemoteByType and unitCOnfiglist
                serviceRemoteMap.put(serviceTemplate, serviceRemoteFactory.createAndInitServiceRemote(serviceTemplate.getType(), unitConfigsByService));
                unitConfigsByService.clear();
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        try {
            MultiException.ExceptionStack exceptionStack = null;
            for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
                try {
                    remote.activate();
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(remote, ex, exceptionStack);
                }
            }
            MultiException.checkAndThrow("Could not activate all internal service remotes!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate unit group remote!", ex);
        }
    }

    @Override
    public boolean isActive() {
        return serviceRemoteMap.values().stream().noneMatch((remote) -> (!remote.isActive()));
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
            remote.deactivate();
        }
    }

    @Override
    public void waitForData(long timeout, TimeUnit timeUnit) throws NotAvailableException, InterruptedException {
        //todo reimplement with respect to the given timeout.
        try {
            super.waitForData(timeout, timeUnit);
            for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
                remote.waitForData(timeout, timeUnit);
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ServiceData", ex);
        }
    }

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        // super.waitForData();
        // disabled because this is not yet a real remote!
        for (AbstractServiceRemote remote : serviceRemoteMap.values()) {
            remote.waitForData();
        }
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

    private boolean unitHasService(UnitConfig unitConfig, ServiceTemplate serviceTemplate) {
        // todo: why does the bottom check not work anymore?
        return unitConfig.getServiceConfigList().stream().anyMatch((serviceConfig) -> (serviceConfig.getServiceTemplate().getType().equals(serviceTemplate.getType())
                && serviceConfig.getServiceTemplate().getPattern().equals(serviceTemplate.getPattern())));
        //return unitConfig.getServiceConfigList().stream().anyMatch((serviceConfig) -> (serviceConfig.getServiceTemplate().equals(serviceTemplate)));
    }
}
