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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.dal.lib.layer.service.operation.BrightnessOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ColorOperationService;
import org.dc.bco.dal.lib.layer.service.operation.DimOperationService;
import org.dc.bco.dal.lib.layer.service.operation.OpeningRatioOperationService;
import org.dc.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.dc.bco.dal.lib.layer.service.operation.ShutterOperationService;
import org.dc.bco.dal.lib.layer.service.operation.StandbyOperationService;
import org.dc.bco.dal.lib.layer.service.operation.TargetTemperatureOperationService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.state.ShutterStateType;
import rst.homeautomation.state.StandbyStateType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class ServiceFactoryMock implements ServiceFactory {

    private final static ServiceFactory instance = new ServiceFactoryMock();

    public static ServiceFactory getInstance() {
        return instance;
    }

    @Override
    public <UNIT extends BrightnessOperationService & Unit> BrightnessOperationService newBrightnessService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new BrightnessOperationService() {

            @Override
            public Double getBrightness() throws CouldNotPerformException, InterruptedException {
                return ((BrightnessOperationService) unit).getBrightness();
            }

            @Override
            public Future<Void> setBrightness(Double brightness) throws CouldNotPerformException {
                return update(brightness, unit);
            }
        };
    }

    @Override
    public <UNIT extends ColorOperationService & Unit> ColorOperationService newColorService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new ColorOperationService() {

            @Override
            public HSVColor getColor() throws CouldNotPerformException, InterruptedException {
                return ((ColorOperationService) unit).getColor();
            }

            @Override
            public Future<Void> setColor(HSVColor color) throws CouldNotPerformException {
                return update(color, unit);
            }
        };
    }

    @Override
    public <UNIT extends PowerOperationService & Unit> PowerOperationService newPowerService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new PowerOperationService() {

            @Override
            public PowerState getPower() throws CouldNotPerformException, InterruptedException {
                return ((PowerOperationService) unit).getPower();
            }

            @Override
            public Future<Void> setPower(PowerStateType.PowerState state) throws CouldNotPerformException {
                return update(state, unit);
            }
        };
    }

    @Override
    public <UNIT extends OpeningRatioOperationService & Unit> OpeningRatioOperationService newOpeningRatioService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new OpeningRatioOperationService() {

            @Override
            public Double getOpeningRatio() throws CouldNotPerformException, InterruptedException {
                return ((OpeningRatioOperationService) unit).getOpeningRatio();
            }

            @Override
            public Future<Void> setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
                return update(openingRatio, unit);
            }
        };
    }

    @Override
    public <UNIT extends ShutterOperationService & Unit> ShutterOperationService newShutterService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new ShutterOperationService() {

            @Override
            public ShutterStateType.ShutterState getShutter() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Future<Void> setShutter(ShutterStateType.ShutterState state) throws CouldNotPerformException {
                return update(state, unit);
            }
        };
    }

    @Override
    public <UNIT extends DimOperationService & Unit> DimOperationService newDimmService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new DimOperationService() {

            @Override
            public Double getDim() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Future<Void> setDim(Double dim) throws CouldNotPerformException {
                return update(dim, unit);
            }
        };
    }

    @Override
    public <UNIT extends StandbyOperationService & Unit> StandbyOperationService newStandbyService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new StandbyOperationService() {

            @Override
            public StandbyStateType.StandbyState getStandby() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Future<Void> setStandby(StandbyStateType.StandbyState state) throws CouldNotPerformException {
                return update(state, unit);
            }
        };
    }

    @Override
    public <UNIT extends TargetTemperatureOperationService & Unit> TargetTemperatureOperationService newTargetTemperatureService(final UNIT unit) throws org.dc.jul.exception.InstantiationException {
        return new TargetTemperatureOperationService() {

            @Override
            public Double getTargetTemperature() throws CouldNotPerformException {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Future<Void> setTargetTemperature(Double value) throws CouldNotPerformException {
                return update(value, unit);
            }
        };
    }

    private static <ARGUMENT extends Object> CompletableFuture<Void> update(final ARGUMENT argument, final Unit unit) throws CouldNotPerformException {
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
            CompletableFuture f;
            f.
            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not call remote Message[]", ex);
        }
    }
}
