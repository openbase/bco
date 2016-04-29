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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.bco.dal.lib.layer.service.DimService;
import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.bco.dal.lib.layer.service.provider.MotionProvider;
import org.dc.bco.dal.lib.layer.service.provider.PowerConsumptionProvider;
import org.dc.bco.dal.lib.layer.service.provider.SmokeAlarmStateProvider;
import org.dc.bco.dal.lib.layer.service.provider.SmokeStateProvider;
import org.dc.bco.dal.lib.layer.service.provider.TamperProvider;
import org.dc.bco.dal.lib.layer.service.provider.TemperatureProvider;
import org.dc.bco.dal.remote.unit.UnitRemote;
import org.dc.bco.dal.remote.unit.UnitRemoteFactory;
import org.dc.bco.dal.remote.unit.UnitRemoteFactoryImpl;
import org.dc.bco.manager.location.lib.Location;
import org.dc.bco.manager.location.lib.LocationController;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import org.dc.jul.extension.rsb.com.AbstractConfigurableController;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.pattern.Observable;
import org.dc.jul.pattern.Observer;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationDataType.LocationData;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class LocationControllerImpl extends AbstractConfigurableController<LocationData, LocationData.Builder, LocationConfig> implements LocationController {

    private final UnitRemoteFactory factory;
    private final Map<String, UnitRemote> unitRemoteMap;
    private Map<ServiceType, Collection<? extends Service>> serviceMap;
    private List<String> originalUnitIdList;

    public LocationControllerImpl(final LocationConfig config) throws InstantiationException {
        super(LocationData.newBuilder());
        this.factory = UnitRemoteFactoryImpl.getInstance();
        this.unitRemoteMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
    }

    private boolean isSupportedServiceType(ServiceType serviceType) {
        switch (serviceType) {
            case BRIGHTNESS_SERVICE:
            case COLOR_SERVICE:
            case DIM_SERVICE:
            case MOTION_PROVIDER:
            case OPENING_RATIO_SERVICE:
            case POWER_CONSUMPTION_PROVIDER:
            case POWER_SERVICE:
            case SHUTTER_SERVICE:
            case SMOKE_ALARM_STATE_PROVIDER:
            case SMOKE_STATE_PROVIDER:
            case STANDBY_SERVICE:
            case TAMPER_PROVIDER:
            case TARGET_TEMPERATURE_SERVICE:
            case TEMPERATURE_PROVIDER:
                return true;
            default:
                return false;
        }
    }

    private boolean isSupportedServiceType(List<ServiceType> serviceTypes) {
        if (serviceTypes.stream().anyMatch((serviceType) -> (isSupportedServiceType(serviceType)))) {
            return true;
        }
        return false;
    }

    private void updateServiceMap(ServiceType serviceType, UnitRemote remote, String unitId) {
        switch (serviceType) {
            case BRIGHTNESS_SERVICE:
                ((ArrayList<BrightnessService>) serviceMap.get(ServiceType.BRIGHTNESS_SERVICE)).add((BrightnessService) remote);
//                brightnessServiceMap.put(unitId, (BrightnessService) remote);
                remote.addObserver(new Observer() {

                    @Override
                    public void update(final Observable source, Object data) throws Exception {
                        setField("brightness", data);
                        notify();
                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                            dataBuilder.getInternalBuilder().setBrightness(getBrightness());
                        } catch (Exception ex) {
                            throw new CouldNotPerformException("Could not apply data change!", ex);
                        }
                    }
                });
                break;
        }
    }

    @Override
    public void init(final LocationConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            originalUnitIdList = config.getUnitIdList();
            for (ServiceType serviceType : ServiceType.values()) {
                if (isSupportedServiceType(serviceType)) {
                    serviceMap.put(serviceType, new ArrayList<>());
                }
            }
            // instantiate unit remotes and add them to lists with their according services
            // add observer to the unit remotes which call according getter
            // getter have to update the location data type
            DeviceRegistry deviceRegistry = LocationManagerController.getInstance().getDeviceRegistry();
            for (UnitConfig unitConfig : deviceRegistry.getUnitConfigs()) {
                if (config.getUnitIdList().contains(unitConfig.getId())) {
                    List<ServiceType> serviceTypes = deviceRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTypeList();
                    // ignore units that do not have any service supported by a location
                    if (!isSupportedServiceType(serviceTypes)) {
                        continue;
                    }

                    UnitRemote unitRemote = factory.newInitializedInstance(unitConfig);
                    unitRemoteMap.put(unitConfig.getId(), unitRemote);
                    unitRemote.addObserver(new Observer() {

                        @Override
                        public void update(final Observable source, Object data) throws Exception {
                            try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                                dataBuilder.getInternalBuilder().setBrightness(getBrightness());
                                dataBuilder.getInternalBuilder().setColor(getColor());
                                dataBuilder.getInternalBuilder().setDimValue(getDim());
                                dataBuilder.getInternalBuilder().setMotionState(getMotion());
                                dataBuilder.getInternalBuilder().setOpeningRatio(getOpeningRatio());
                                dataBuilder.getInternalBuilder().setPowerConsumptionState(getPowerConsumption());
                                dataBuilder.getInternalBuilder().setPowerState(getPower());
                                dataBuilder.getInternalBuilder().setShutterState(getShutter());
                                dataBuilder.getInternalBuilder().setSmokeAlarmState(getSmokeAlarmState());
                                dataBuilder.getInternalBuilder().setSmokeState(getSmokeState());
                                dataBuilder.getInternalBuilder().setStandbyState(getStandby());
                                dataBuilder.getInternalBuilder().setTamperState(getTamper());
                                dataBuilder.getInternalBuilder().setTargetTemperature(getTargetTemperature());
                                dataBuilder.getInternalBuilder().setTemperature(getTemperature());
                            } catch (Exception ex) {
                                throw new CouldNotPerformException("Could not apply data change!", ex);
                            }
                        }
                    });
                    for (ServiceType serviceType : serviceTypes) {
//                        serviceMap.get(serviceType).add(unitRemote);
                        switch (serviceType) {
                            case BRIGHTNESS_SERVICE:
//                                brightnessServiceMap.put(unitConfig.getId(), (BrightnessService) unitRemote);
                                unitRemote.addObserver(new Observer() {

                                    @Override
                                    public void update(final Observable source, Object data) throws Exception {
                                        try (ClosableDataBuilder<LocationData.Builder> dataBuilder = getDataBuilder(this)) {
                                            dataBuilder.getInternalBuilder().setBrightness(getBrightness());
                                        } catch (Exception ex) {
                                            throw new CouldNotPerformException("Could not apply data change!", ex);
                                        }
                                    }
                                });
                                break;
                        }
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public LocationConfig updateConfig(final LocationConfig config) throws CouldNotPerformException, InterruptedException {
        //TODO: service remotes have to be updated when the units in a location change
        // search unit ids for new ones and removed ones and create/delete remotes accordingly
        List<String> newUnitIdList = new ArrayList<>(config.getUnitIdList());
        for (String originalId : originalUnitIdList) {
            if (config.getUnitIdList().contains(originalId)) {
                newUnitIdList.remove(originalId);
            } else {
                unitRemoteMap.get(originalId).deactivate();
                unitRemoteMap.remove(originalId);
                for (Collection<? extends Service> test : serviceMap.values()) {
                    Collection a = new ArrayList<>(test);
                    for (Object b : a) {
                        if (((UnitRemote) b).getId().equals(originalId)) {
                            test.remove(b);
                        }
                    }
                }
                //TODO: remove from the according service maps as well
            }
        }
        for (String newUnitId : newUnitIdList) {
            // create new remote and add to the according maps
        }
        originalUnitIdList = config.getUnitIdList();
        return super.updateConfig(config);
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        // activate all unit remotes
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();
        // deavtivate all unit remotes
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
    public Collection<ColorService> getColorStateOperationServices() {
        return (Collection<ColorService>) serviceMap.get(ServiceType.COLOR_SERVICE);
    }

    @Override
    public Collection<BrightnessService> getBrightnessStateOperationServices() {
        return (Collection<BrightnessService>) serviceMap.get(ServiceType.BRIGHTNESS_SERVICE);
    }

    @Override
    public Collection<DimService> getDimStateOperationServices() {
        return (Collection<DimService>) serviceMap.get(ServiceType.DIM_SERVICE);
    }

    @Override
    public Collection<OpeningRatioService> getOpeningRatioStateOperationServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<PowerService> getPowerStateOperationServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<ShutterService> getShutterStateOperationServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<StandbyService> getStandbyStateOperationServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<TargetTemperatureService> getTargetTemperatureStateOperationServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<MotionProvider> getMotionStateProviderServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<SmokeAlarmStateProvider> getSmokeAlarmStateProviderServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<SmokeStateProvider> getSmokeStateProviderServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<TemperatureProvider> getTemperatureStateProviderServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<PowerConsumptionProvider> getPowerConsumptionStateProviderServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<TamperProvider> getTamperStateProviderServices() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
