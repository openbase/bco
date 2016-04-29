package org.dc.bco.manager.location.remote;

import org.dc.bco.manager.location.lib.Location;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.dc.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
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
 * Copyright (C) 2015 - 2016 DivineCooperation
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

    @Override
    public void notifyUpdated(LocationData data) throws CouldNotPerformException {
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
    public void setBrightness(Double brightness) throws CouldNotPerformException {
        RPCHelper.callRemoteMethod(brightness, this);
    }

    @Override
    public Double getBrightness() throws CouldNotPerformException {
        return getData().getBrightness();
    }

    @Override
    public void setColor(HSVColor color) throws CouldNotPerformException {
        RPCHelper.callRemoteMethod(color, this);
    }

    @Override
    public HSVColor getColor() throws CouldNotPerformException {
        return getData().getColor();
    }

    @Override
    public void setDim(Double dim) throws CouldNotPerformException {
        RPCHelper.callRemoteMethod(dim, this);
    }

    @Override
    public Double getDim() throws CouldNotPerformException {
        return getData().getDimValue();
    }

    @Override
    public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        RPCHelper.callRemoteMethod(openingRatio, this);
    }

    @Override
    public Double getOpeningRatio() throws CouldNotPerformException {
        return getData().getOpeningRatio();
    }

    @Override
    public void setPower(PowerState state) throws CouldNotPerformException {
        RPCHelper.callRemoteMethod(state, this);
    }

    @Override
    public PowerState getPower() throws CouldNotPerformException {
        return getData().getPowerState();
    }

    @Override
    public void setShutter(ShutterState state) throws CouldNotPerformException {
        RPCHelper.callRemoteMethod(state, this);
    }

    @Override
    public ShutterState getShutter() throws CouldNotPerformException {
        return getData().getShutterState();
    }

    @Override
    public void setStandby(StandbyState state) throws CouldNotPerformException {
        RPCHelper.callRemoteMethod(state, this);
    }

    @Override
    public StandbyState getStandby() throws CouldNotPerformException {
        return getData().getStandbyState();
    }

    @Override
    public void setTargetTemperature(Double value) throws CouldNotPerformException {
        RPCHelper.callRemoteMethod(value, this);
    }

    @Override
    public Double getTargetTemperature() throws CouldNotPerformException {
        return getData().getTargetTemperature();
    }

    @Override
    public MotionState getMotion() throws CouldNotPerformException {
        return getData().getMotionState();
    }

    @Override
    public AlarmState getSmokeAlarmState() throws CouldNotPerformException {
        return getData().getSmokeAlarmState();
    }

    @Override
    public SmokeState getSmokeState() throws CouldNotPerformException {
        return getData().getSmokeState();
    }

    @Override
    public Double getTemperature() throws CouldNotPerformException {
        return getData().getTemperature();
    }

    @Override
    public PowerConsumptionState getPowerConsumption() throws CouldNotPerformException {
        return getData().getPowerConsumptionState();
    }

    @Override
    public TamperState getTamper() throws CouldNotPerformException {
        return getData().getTamperState();
    }
}
