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
import java.util.Map.Entry;
import org.dc.bco.dal.remote.service.AbstractServiceRemote;
import org.dc.bco.dal.remote.service.BrightnessServiceRemote;
import org.dc.bco.dal.remote.service.ColorServiceRemote;
import org.dc.bco.dal.remote.service.DimServiceRemote;
import org.dc.bco.dal.remote.service.MotionProviderRemote;
import org.dc.bco.dal.remote.service.OpeningRatioServiceRemote;
import org.dc.bco.dal.remote.service.PowerConsumptionProviderRemote;
import org.dc.bco.dal.remote.service.PowerServiceRemote;
import org.dc.bco.dal.remote.service.ServiceRemoteFactoryImpl;
import org.dc.bco.dal.remote.service.ShutterServiceRemote;
import org.dc.bco.dal.remote.service.SmokeAlarmStateProviderRemote;
import org.dc.bco.dal.remote.service.SmokeStateProviderRemote;
import org.dc.bco.dal.remote.service.StandbyServiceRemote;
import org.dc.bco.dal.remote.service.TargetTemperatureServiceRemote;
import org.dc.bco.dal.remote.service.TemperatureProviderRemote;
import org.dc.bco.manager.location.lib.Location;
import org.dc.bco.manager.location.lib.LocationController;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.com.AbstractConfigurableController;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.AlarmStateType;
import rst.homeautomation.state.MotionStateType;
import rst.homeautomation.state.PowerConsumptionStateType;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.state.ShutterStateType;
import rst.homeautomation.state.SmokeStateType;
import rst.homeautomation.state.StandbyStateType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationDataType.LocationData;
import rst.vision.HSVColorType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class LocationControllerImpl extends AbstractConfigurableController<LocationData, LocationData.Builder, LocationConfig> implements LocationController {

    private final Map<ServiceType, AbstractServiceRemote> serviceRemoteMap;

    public LocationControllerImpl(final LocationConfig config) throws InstantiationException {
        super(LocationData.newBuilder());
        serviceRemoteMap = new HashMap<>();
    }

    @Override
    public void init(final LocationConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            Map<ServiceType, List<UnitConfig>> unitConfigByServiceMap = new HashMap<>();
            for (ServiceType serviceType : ServiceType.values()) {
                unitConfigByServiceMap.put(serviceType, new ArrayList<>());
            }
            DeviceRegistry deviceRegistry = LocationManagerController.getInstance().getDeviceRegistry();
            for (UnitConfig unitConfig : deviceRegistry.getUnitConfigs()) {
                if (config.getUnitIdList().contains(unitConfig.getId())) {
                    for (ServiceType serviceType : deviceRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTypeList()) {
                        unitConfigByServiceMap.get(serviceType).add(unitConfig);
                    }
                }
            }
            for (Entry<ServiceType, List<UnitConfig>> entry : unitConfigByServiceMap.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                serviceRemoteMap.put(entry.getKey(), ServiceRemoteFactoryImpl.getInstance().createAndInitServiceRemote(entry.getKey(), entry.getValue()));
            }
        } catch (CouldNotPerformException ex) {

        }
    }

    @Override
    public LocationConfig updateConfig(final LocationConfig config) throws CouldNotPerformException, InterruptedException {
        //TODO: service remotes have to be updated when the units in a location change
        return super.updateConfig(config);
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
    public void registerMethods(RSBLocalServerInterface server) throws CouldNotPerformException {
        RPCHelper.registerInterface(Location.class, this, server);
    }

    @Override
    public String getLabel() throws NotAvailableException {
        return getConfig().getLabel();
    }

    @Override
    public void setBrightness(Double brightness) throws CouldNotPerformException {
        try {
            ((BrightnessServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_SERVICE)).setBrightness(brightness);
        } catch (NullPointerException ex) {
            throw new NotAvailableException("brightnessServiceRemote");
        }
    }

    @Override
    public Double getBrightness() throws CouldNotPerformException {
        try {
            return ((BrightnessServiceRemote) serviceRemoteMap.get(ServiceType.BRIGHTNESS_SERVICE)).getBrightness();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("brightnessServiceRemote");
        }
    }

    @Override
    public void setColor(HSVColorType.HSVColor color) throws CouldNotPerformException {
        try {
            ((ColorServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_SERVICE)).setColor(color);
        } catch (NullPointerException ex) {
            throw new NotAvailableException("colorServiceRemote");
        }
    }

    @Override
    public HSVColorType.HSVColor getColor() throws CouldNotPerformException {
        try {
            return ((ColorServiceRemote) serviceRemoteMap.get(ServiceType.COLOR_SERVICE)).getColor();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("colorServiceRemote");
        }
    }

    @Override
    public void setDim(Double dim) throws CouldNotPerformException {
        try {
            ((DimServiceRemote) serviceRemoteMap.get(ServiceType.DIM_SERVICE)).setDim(dim);
        } catch (NullPointerException ex) {
            throw new NotAvailableException("dimServiceRemote");
        }
    }

    @Override
    public Double getDim() throws CouldNotPerformException {
        try {
            return ((DimServiceRemote) serviceRemoteMap.get(ServiceType.DIM_SERVICE)).getDim();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("dimServiceRemote");
        }
    }

    @Override
    public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        try {
            ((OpeningRatioServiceRemote) serviceRemoteMap.get(ServiceType.OPENING_RATIO_SERVICE)).setOpeningRatio(openingRatio);
        } catch (NullPointerException ex) {
            throw new NotAvailableException("openingRatioServiceRemote");
        }
    }

    @Override
    public Double getOpeningRatio() throws CouldNotPerformException {
        try {
            return ((OpeningRatioServiceRemote) serviceRemoteMap.get(ServiceType.OPENING_RATIO_SERVICE)).getOpeningRatio();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("openingRatioServiceRemote");
        }
    }

    @Override
    public void setPower(PowerStateType.PowerState state) throws CouldNotPerformException {
        try {
            ((PowerServiceRemote) serviceRemoteMap.get(ServiceType.POWER_SERVICE)).setPower(state);
        } catch (NullPointerException ex) {
            throw new NotAvailableException("powerServiceRemote");
        }
    }

    @Override
    public PowerStateType.PowerState getPower() throws CouldNotPerformException {
        try {
            return ((PowerServiceRemote) serviceRemoteMap.get(ServiceType.POWER_SERVICE)).getPower();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("powerServiceRemote");
        }
    }

    @Override
    public void setShutter(ShutterStateType.ShutterState state) throws CouldNotPerformException {
        try {
            ((ShutterServiceRemote) serviceRemoteMap.get(ServiceType.SHUTTER_SERVICE)).setShutter(state);
        } catch (NullPointerException ex) {
            throw new NotAvailableException("shutterServiceRemote");
        }
    }

    @Override
    public ShutterStateType.ShutterState getShutter() throws CouldNotPerformException {
        try {
            return ((ShutterServiceRemote) serviceRemoteMap.get(ServiceType.SHUTTER_SERVICE)).getShutter();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("shutterServiceRemote");
        }
    }

    @Override
    public void setStandby(StandbyStateType.StandbyState state) throws CouldNotPerformException {
        try {
            ((StandbyServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_SERVICE)).setStandby(state);
        } catch (NullPointerException ex) {
            throw new NotAvailableException("standbyServiceRemote");
        }
    }

    @Override
    public StandbyStateType.StandbyState getStandby() throws CouldNotPerformException {
        try {
            return ((StandbyServiceRemote) serviceRemoteMap.get(ServiceType.STANDBY_SERVICE)).getStandby();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("standbyServiceRemote");
        }
    }

    @Override
    public void setTargetTemperature(Double value) throws CouldNotPerformException {
        try {
            ((TargetTemperatureServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_SERVICE)).setTargetTemperature(value);
        } catch (NullPointerException ex) {
            throw new NotAvailableException("targetTemperatureServiceRemote");
        }
    }

    @Override
    public Double getTargetTemperature() throws CouldNotPerformException {
        try {
            return ((TargetTemperatureServiceRemote) serviceRemoteMap.get(ServiceType.TARGET_TEMPERATURE_SERVICE)).getTargetTemperature();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("targetTemperatureServiceRemote");
        }
    }

    @Override
    public MotionStateType.MotionState getMotion() throws CouldNotPerformException {
        try {
            return ((MotionProviderRemote) serviceRemoteMap.get(ServiceType.MOTION_PROVIDER)).getMotion();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("motionProviderRemote");
        }
    }

    @Override
    public AlarmStateType.AlarmState getSmokeAlarmState() throws CouldNotPerformException {
        try {
            return ((SmokeAlarmStateProviderRemote) serviceRemoteMap.get(ServiceType.SMOKE_ALARM_STATE_PROVIDER)).getSmokeAlarmState();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("smokeAlarmStateProviderRemote");
        }
    }

    @Override
    public SmokeStateType.SmokeState getSmokeState() throws CouldNotPerformException {
        try {
            return ((SmokeStateProviderRemote) serviceRemoteMap.get(ServiceType.SMOKE_STATE_PROVIDER)).getSmokeState();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("smokeStateProviderRemote");
        }
    }

    @Override
    public Double getTemperature() throws CouldNotPerformException {
        try {
            return ((TemperatureProviderRemote) serviceRemoteMap.get(ServiceType.TEMPERATURE_PROVIDER)).getTemperature();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("temperatureProviderRemote");
        }
    }

    @Override
    public PowerConsumptionStateType.PowerConsumptionState getPowerConsumption() throws CouldNotPerformException {
        try {
            return ((PowerConsumptionProviderRemote) serviceRemoteMap.get(ServiceType.POWER_CONSUMPTION_PROVIDER)).getPowerConsumption();
        } catch (NullPointerException ex) {
            throw new NotAvailableException("temperatureProviderRemote");
        }
    }
}
