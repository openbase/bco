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
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.OpeningRatioOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ShutterOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureOperationService;
import org.openbase.bco.dal.remote.service.AbstractServiceRemote;
import org.openbase.bco.dal.remote.service.BrightnessServiceRemote;
import org.openbase.bco.dal.remote.service.ColorServiceRemote;
import org.openbase.bco.dal.remote.service.OpeningRatioServiceRemote;
import org.openbase.bco.dal.remote.service.PowerServiceRemote;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactory;
import org.openbase.bco.dal.remote.service.ServiceRemoteFactoryImpl;
import org.openbase.bco.dal.remote.service.ShutterServiceRemote;
import org.openbase.bco.dal.remote.service.StandbyServiceRemote;
import org.openbase.bco.dal.remote.service.TargetTemperatureServiceRemote;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.state.ShutterStateType;
import rst.homeautomation.state.StandbyStateType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitGroupConfigType.UnitGroupConfig;
import rst.vision.HSVColorType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class UnitGroupRemote extends AbstractIdentifiableRemote<UnitGroupConfig> implements BrightnessOperationService, ColorOperationService, OpeningRatioOperationService, PowerOperationService, ShutterOperationService, StandbyOperationService, TargetTemperatureOperationService {

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
    public Future<Void> setBrightness(Double brightness) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.BRIGHTNESS_SERVICE);
        return ((BrightnessServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.BRIGHTNESS_SERVICE)).setBrightness(brightness);
    }

    @Override
    public Double getBrightness() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.BRIGHTNESS_SERVICE);
        return ((BrightnessServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.BRIGHTNESS_SERVICE)).getBrightness();
    }

    @Override
    public Future<Void> setColor(HSVColorType.HSVColor color) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.COLOR_SERVICE);
        return ((ColorServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.COLOR_SERVICE)).setColor(color);
    }

    @Override
    public HSVColorType.HSVColor getColor() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.COLOR_SERVICE);
        return ((ColorServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.COLOR_SERVICE)).getColor();
    }

    @Override
    public Future<Void> setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.OPENING_RATIO_SERVICE);
        return ((OpeningRatioServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.OPENING_RATIO_SERVICE)).setOpeningRatio(openingRatio);
    }

    @Override
    public Double getOpeningRatio() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.OPENING_RATIO_SERVICE);
        return ((OpeningRatioServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.OPENING_RATIO_SERVICE)).getOpeningRatio();
    }

    @Override
    public Future<Void> setPower(PowerStateType.PowerState state) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.POWER_SERVICE);
        return ((PowerServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.POWER_SERVICE)).setPower(state);
    }

    @Override
    public PowerStateType.PowerState getPower() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.POWER_SERVICE);
        return ((PowerServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.POWER_SERVICE)).getPower();
    }

    @Override
    public Future<Void> setShutter(ShutterStateType.ShutterState state) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.SHUTTER_SERVICE);
        return ((ShutterServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.SHUTTER_SERVICE)).setShutter(state);
    }

    @Override
    public ShutterStateType.ShutterState getShutter() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.SHUTTER_SERVICE);
        return ((ShutterServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.SHUTTER_SERVICE)).getShutter();
    }

    @Override
    public Future<Void> setStandby(StandbyStateType.StandbyState state) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.STANDBY_SERVICE);
        return ((StandbyServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.STANDBY_SERVICE)).setStandby(state);
    }

    @Override
    public StandbyStateType.StandbyState getStandby() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.STANDBY_SERVICE);
        return ((StandbyServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.STANDBY_SERVICE)).getStandby();
    }

    @Override
    public Future<Void> setTargetTemperature(Double value) throws CouldNotPerformException {
        testServiceAvailability(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_SERVICE);
        return ((TargetTemperatureServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_SERVICE)).setTargetTemperature(value);
    }

    @Override
    public Double getTargetTemperature() throws NotAvailableException {
        testServiceAvailability(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_SERVICE);
        return ((TargetTemperatureServiceRemote) serviceRemoteMap.get(ServiceTemplate.ServiceType.TARGET_TEMPERATURE_SERVICE)).getTargetTemperature();
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
        for (ServiceTemplate.ServiceType serviceType : unitGroupConfig.getServiceTypeList()) {
            for (UnitConfig unitConfig : unitConfigs) {
                if (unitHasService(unitConfig, serviceType)) {
                    unitConfigsByService.add(unitConfig);
                }
            }
            // create serviceRemoteByType and unitCOnfiglist
            serviceRemoteMap.put(serviceType, serviceRemoteFactory.createAndInitServiceRemote(serviceType, unitConfigsByService));
            unitConfigsByService.clear();
        }
    }

    private boolean unitHasService(UnitConfig unitConfig, ServiceTemplate.ServiceType serviceType) {
        for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            if (serviceConfig.getType() == serviceType) {
                return true;
            }
        }
        return false;
    }
}
