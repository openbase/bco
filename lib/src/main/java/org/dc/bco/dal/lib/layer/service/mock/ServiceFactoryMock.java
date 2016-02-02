package org.dc.bco.dal.lib.layer.service.mock;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.bco.dal.lib.layer.service.DimService;
import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.dal.lib.layer.service.ServiceType;
import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.state.ShutterStateType;
import rst.homeautomation.state.StandbyStateType;
import rst.vision.HSVColorType;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class ServiceFactoryMock implements ServiceFactory {

    private final static ServiceFactory instance = new ServiceFactoryMock();

    public static ServiceFactory getInstance() {
        return instance;
    }

    @Override
    public <UNIT extends BrightnessService & Unit> BrightnessService newBrightnessService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new BrightnessService() {

            @Override
            public void setBrightness(Double brightness) throws CouldNotPerformException {
                update(brightness, unit);
            }

            @Override
            public ServiceType getServiceType() {
                return ServiceType.BRIGHTNESS;
            }

            @Override
            public ServiceConfig getServiceConfig() {
                return null;
            }

            @Override
            public Double getBrightness() throws CouldNotPerformException {
                return ((BrightnessService) unit).getBrightness();
            }
        };
    }

    @Override
    public <UNIT extends ColorService & Unit> ColorService newColorService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new ColorService() {

            @Override
            public void setColor(HSVColorType.HSVColor color) throws CouldNotPerformException {
                update(color, unit);
            }

            @Override
            public HSVColorType.HSVColor getColor() throws CouldNotPerformException {
                return ((ColorService) unit).getColor();
            }

            @Override
            public ServiceType getServiceType() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ServiceConfig getServiceConfig() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    @Override
    public <UNIT extends PowerService & Unit> PowerService newPowerService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new PowerService() {

            @Override
            public void setPower(PowerStateType.PowerState.State state) throws CouldNotPerformException {
                update(state, unit);
            }

            @Override
            public ServiceType getServiceType() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ServiceConfig getServiceConfig() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public PowerStateType.PowerState getPower() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    @Override
    public <UNIT extends OpeningRatioService & Unit> OpeningRatioService newOpeningRatioService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new OpeningRatioService() {

            @Override
            public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
                update(openingRatio, unit);
            }

            @Override
            public ServiceType getServiceType() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ServiceConfig getServiceConfig() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Double getOpeningRatio() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    @Override
    public <UNIT extends ShutterService & Unit> ShutterService newShutterService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new ShutterService() {

            @Override
            public void setShutter(ShutterStateType.ShutterState.State state) throws CouldNotPerformException {
                update(state, unit);
            }

            @Override
            public ServiceType getServiceType() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ServiceConfig getServiceConfig() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ShutterStateType.ShutterState getShutter() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    @Override
    public <UNIT extends DimService & Unit> DimService newDimmService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new DimService() {

            @Override
            public void setDim(Double dim) throws CouldNotPerformException {
                update(dim, unit);
            }

            @Override
            public ServiceType getServiceType() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ServiceConfig getServiceConfig() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Double getDim() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    @Override
    public <UNIT extends StandbyService & Unit> StandbyService newStandbyService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new StandbyService() {

            @Override
            public void setStandby(StandbyStateType.StandbyState.State state) throws CouldNotPerformException {
                update(state, unit);
            }

            @Override
            public ServiceType getServiceType() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ServiceConfig getServiceConfig() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public StandbyStateType.StandbyState getStandby() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    @Override
    public <UNIT extends TargetTemperatureService & Unit> TargetTemperatureService newTargetTemperatureService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new TargetTemperatureService() {

            @Override
            public void setTargetTemperature(Double value) throws CouldNotPerformException {
                update(value, unit);
            }

            @Override
            public ServiceType getServiceType() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public ServiceConfig getServiceConfig() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Double getTargetTemperature() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
    }

    private static <ARGUMENT extends Object> void update(final ARGUMENT argument, final Unit unit) throws CouldNotPerformException {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace == null) {
                throw new NotAvailableException("method stack");
            } else if (stackTrace.length == 0) {
                throw new InvalidStateException("Could not detect method stack!");
            }
            String methodName;
            try {
                methodName = stackTrace[3].getMethodName().replaceFirst("set", "update");
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not detect update method name!", ex);
            }
            unit.getClass().getMethod(methodName, argument.getClass()).invoke(unit, argument);
//            return (Future<RETURN>) remote.callMethodAsync(methodName, argument);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not call remote Message[]", ex);
        }
    }
}
