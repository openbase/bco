package org.openbase.bco.manager.location.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.bco.manager.location.lib.Location;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.bco.registry.location.remote.CachedLocationRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.ShutterStateType.ShutterState;
import rst.homeautomation.state.SmokeStateType.SmokeState;
import rst.homeautomation.state.StandbyStateType.StandbyState;
import rst.homeautomation.state.TamperStateType.TamperState;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationDataType.LocationData;
import rst.vision.HSVColorType.HSVColor;

/*
 * #%L
 * COMA LocationManager Remote
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class LocationRemote extends AbstractConfigurableRemote<LocationData, LocationConfig> implements Location {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ShutterState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSVColor.getDefaultInstance()));
    }

    public LocationRemote() {
        super(LocationData.class, LocationConfig.class);
    }

    @Override
    public void notifyDataUpdate(LocationData data) throws CouldNotPerformException {
    }

    @Override
    public String getLabel() throws NotAvailableException {
        try {
            if (config == null) {
                throw new NotAvailableException("locationConfig");
            }
            return config.getLabel();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("label", ex);
        }
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        CachedLocationRegistryRemote.waitForData();
        super.activate();
    }

    @Override
    public Future<Void> setBrightness(Double brightness) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(brightness, this, Void.class);
    }

    @Override
    public Double getBrightness() throws NotAvailableException {
        try {
            return getData().getBrightness();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Brightness", ex);
        }
    }

    @Override
    public Future<Void> setColor(HSVColor color) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(color, this, Void.class);
    }

    @Override
    public HSVColor getColor() throws NotAvailableException {
        try {
            return getData().getColor();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Color", ex);
        }
    }

    @Override
    public Future<Void> setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(openingRatio, this, Void.class);
    }

    @Override
    public Double getOpeningRatio() throws NotAvailableException {
        try {
            return getData().getOpeningRatio();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("OpeningRatio", ex);
        }
    }

    public void setPower(PowerState.State state) throws CouldNotPerformException {
        setPower(PowerState.newBuilder().setValue(state).build());
    }

    @Override
    public Future<Void> setPower(PowerState state) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(state, this, Void.class);
    }

    @Override
    public PowerState getPower() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerState", ex);
        }
    }

    @Override
    public Future<Void> setShutter(ShutterState state) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(state, this, Void.class);
    }

    @Override
    public ShutterState getShutter() throws NotAvailableException {
        try {
            return getData().getShutterState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("ShutterState", ex);
        }
    }

    @Override
    public Future<Void> setStandby(StandbyState state) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(state, this, Void.class);
    }

    @Override
    public StandbyState getStandby() throws NotAvailableException {
        try {
            return getData().getStandbyState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("StandbyState", ex);
        }
    }

    @Override
    public Future<Void> setTargetTemperature(Double value) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(value, this, Void.class);
    }

    @Override
    public Double getTargetTemperature() throws NotAvailableException {
        try {
            return getData().getTargetTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TargetTemperature", ex);
        }
    }

    @Override
    public MotionState getMotion() throws NotAvailableException {
        try {
            return getData().getMotionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("MotionState", ex);
        }
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        try {
            return getData().getSmokeAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("AlarmState", ex);
        }
    }

    @Override
    public SmokeState getSmokeState() throws NotAvailableException {
        try {
            return getData().getSmokeState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("SmokeState", ex);
        }
    }

    @Override
    public Double getTemperature() throws NotAvailableException {
        try {
            return getData().getTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Temperature", ex);
        }
    }

    @Override
    public PowerConsumptionState getPowerConsumption() throws NotAvailableException {
        try {
            return getData().getPowerConsumptionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerConsumptionState", ex);
        }
    }

    @Override
    public TamperState getTamper() throws NotAvailableException {
        try {
            return getData().getTamperState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TamperState", ex);
        }
    }

    @Override
    public Future<SceneConfig> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(this, SceneConfig.class);
    }

    @Override
    public Future<Void> restoreSnapshot(final SceneConfig snapshot) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(snapshot, this, Void.class);
    }

    @Override
    public Future<Void> applyAction(ActionConfigType.ActionConfig actionConfig) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(actionConfig, this, Void.class);
    }

    @Override
    public List<String> getNeighborLocationIds() throws CouldNotPerformException {
        List<String> neighborIdList = new ArrayList<>();
        try {
            for (LocationConfig locationConfig : CachedLocationRegistryRemote.getRegistry().getNeighborLocations(getId())) {
                neighborIdList.add(locationConfig.getId());
            }
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Could not get CachedLocationRegistryRemote!", ex);
        }
        return neighborIdList;
    }
}
