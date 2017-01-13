package org.openbase.bco.dal.lib.layer.service.mock;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.IntensityStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.IntensityStateType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.StandbyStateType;
import rst.domotic.state.TemperatureStateType.TemperatureState;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ServiceFactoryMock implements ServiceFactory {

    private final static ServiceFactory instance = new ServiceFactoryMock();

    public static ServiceFactory getInstance() {
        return instance;
    }

    @Override
    public <UNIT extends BrightnessStateOperationService & Unit> BrightnessStateOperationService newBrightnessService(final UNIT unit) throws org.openbase.jul.exception.InstantiationException {
        return new BrightnessStateOperationService() {

            @Override
            public BrightnessState getBrightnessState() throws NotAvailableException {
                return ((BrightnessStateOperationService) unit).getBrightnessState();
            }

            @Override
            public Future<Void> setBrightnessState(BrightnessState brightnessState) throws CouldNotPerformException {
                return update(brightnessState, unit);
            }
        };
    }

    @Override
    public <UNIT extends ColorStateOperationService & Unit> ColorStateOperationService newColorService(final UNIT unit) throws org.openbase.jul.exception.InstantiationException {
        return new ColorStateOperationService() {

            @Override
            public ColorState getColorState() throws NotAvailableException {
                return ((ColorStateOperationService) unit).getColorState();
            }

            @Override
            public Future<Void> setColorState(ColorState colorState) throws CouldNotPerformException {
                return update(colorState, unit);
            }
        };
    }

    @Override
    public <UNIT extends PowerStateOperationService & Unit> PowerStateOperationService newPowerService(final UNIT unit) throws org.openbase.jul.exception.InstantiationException {
        return new PowerStateOperationService() {

            @Override
            public PowerState getPowerState() throws NotAvailableException {
                return ((PowerStateOperationService) unit).getPowerState();
            }

            @Override
            public Future<Void> setPowerState(PowerState powerState) throws CouldNotPerformException {
                return update(powerState, unit);
            }
        };
    }

    @Override
    public <UNIT extends BlindStateOperationService & Unit> BlindStateOperationService newShutterService(final UNIT unit) throws org.openbase.jul.exception.InstantiationException {
        return new BlindStateOperationService() {

            @Override
            public BlindState getBlindState() throws NotAvailableException {
                return ((BlindStateOperationService) unit).getBlindState();
            }

            @Override
            public Future<Void> setBlindState(BlindState blindState) throws CouldNotPerformException {
                return update(blindState, unit);
            }
        };
    }

    @Override
    public <UNIT extends StandbyStateOperationService & Unit> StandbyStateOperationService newStandbyService(final UNIT unit) throws org.openbase.jul.exception.InstantiationException {
        return new StandbyStateOperationService() {

            @Override
            public StandbyStateType.StandbyState getStandbyState() throws NotAvailableException {
                return ((StandbyStateOperationService) unit).getStandbyState();
            }

            @Override
            public Future<Void> setStandbyState(StandbyStateType.StandbyState state) throws CouldNotPerformException {
                return update(state, unit);
            }
        };
    }

    @Override
    public <UNIT extends TargetTemperatureStateOperationService & Unit> TargetTemperatureStateOperationService newTargetTemperatureService(final UNIT unit) throws org.openbase.jul.exception.InstantiationException {
        return new TargetTemperatureStateOperationService() {

            @Override
            public TemperatureState getTargetTemperatureState() throws NotAvailableException {
                return ((TargetTemperatureStateOperationService) unit).getTargetTemperatureState();
            }

            @Override
            public Future<Void> setTargetTemperatureState(TemperatureState temperatureState) throws CouldNotPerformException {
                return update(temperatureState, unit);
            }
        };
    }

    private static <ARGUMENT extends Object> Future<Void> update(final ARGUMENT argument, final Unit unit) throws CouldNotPerformException {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace == null) {
                throw new NotAvailableException("method stack");
            } else if (stackTrace.length == 0) {
                throw new InvalidStateException("Could not detect method stack!");
            }
            String methodName;
            try {
                methodName = stackTrace[3].getMethodName().replaceFirst("set", "update") + "Provider";
            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not detect update method name!", ex);
            }
            unit.getClass().getMethod(methodName, argument.getClass()).invoke(unit, argument);
            return CompletableFuture.completedFuture(null);
        } catch (CouldNotPerformException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new CouldNotPerformException("Could not call remote Message[]", ex);
        }
    }

    @Override
    public <UNIT extends IntensityStateOperationService & Unit> IntensityStateOperationService newIntensityStateService(UNIT unit) throws InstantiationException {
        return new IntensityStateOperationService() {

            @Override
            public Future<Void> setIntensityState(IntensityStateType.IntensityState intensityState) throws CouldNotPerformException {
                return update(intensityState, unit);
            }

            @Override
            public IntensityStateType.IntensityState getIntensityState() throws NotAvailableException {
                return ((IntensityStateOperationService) unit).getIntensityState();
            }

        };
    }
}
