package org.dc.bco.manager.location.remote;

import org.dc.bco.manager.location.lib.Location;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.dc.jul.extension.rsb.com.RPCHelper;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.AlarmStateType;
import rst.homeautomation.state.MotionStateType;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.state.ShutterStateType;
import rst.homeautomation.state.SmokeStateType;
import rst.homeautomation.state.StandbyStateType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationDataType.LocationData;
import rst.vision.HSVColorType;

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
//        return RPCHelper.callRemoteMethod(this, Double.class).get();
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setColor(HSVColorType.HSVColor color) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HSVColorType.HSVColor getColor() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDim(Double dim) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Double getDim() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Double getOpeningRatio() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPower(PowerStateType.PowerState state) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PowerStateType.PowerState getPower() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setShutter(ShutterStateType.ShutterState state) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ShutterStateType.ShutterState getShutter() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setStandby(StandbyStateType.StandbyState state) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public StandbyStateType.StandbyState getStandby() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTargetTemperature(Double value) throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Double getTargetTemperature() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MotionStateType.MotionState getMotion() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AlarmStateType.AlarmState getSmokeAlarmState() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SmokeStateType.SmokeState getSmokeState() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
