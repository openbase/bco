package org.openbase.bco.dal.lib.layer.service.mock;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.StandbyStateType;
import rst.domotic.state.TemperatureStateType.TemperatureState;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ServiceFactoryMock implements ServiceFactory {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ServiceFactoryMock.class);

    private static ServiceFactory instance = new ServiceFactoryMock();

    public static ServiceFactory getInstance() {
        if(instance == null) {
            instance = new ServiceFactoryMock();
        }
        return instance;
    }
    
    private ServiceFactoryMock() {
    }

    @Override
    public <UNIT extends BrightnessStateOperationService & Unit> BrightnessStateOperationService newBrightnessService(final UNIT unit) {
        return new BrightnessStateOperationService() {

            @Override
            public BrightnessState getBrightnessState() throws NotAvailableException {
                return unit.getBrightnessState();
            }

            @Override
            public Future<ActionFuture> setBrightnessState(BrightnessState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.BRIGHTNESS_STATE_SERVICE);
            }
        };
    }

    @Override
    public <UNIT extends ColorStateOperationService & Unit> ColorStateOperationService newColorService(final UNIT unit) {
        return new ColorStateOperationService() {

            @Override
            public ColorState getColorState() throws NotAvailableException {
                return unit.getColorState();
            }

            @Override
            public Future<ActionFuture> setColorState(ColorState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.COLOR_STATE_SERVICE);
            }
        };
    }

    @Override
    public <UNIT extends PowerStateOperationService & Unit> PowerStateOperationService newPowerService(final UNIT unit) {
        return new PowerStateOperationService() {

            @Override
            public PowerState getPowerState() throws NotAvailableException {
                return unit.getPowerState();
            }

            @Override
            public Future<ActionFuture> setPowerState(PowerState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.POWER_STATE_SERVICE);
            }
        };
    }

    @Override
    public <UNIT extends BlindStateOperationService & Unit> BlindStateOperationService newShutterService(final UNIT unit) {
        return new BlindStateOperationService() {

            @Override
            public BlindState getBlindState() throws NotAvailableException {
                return unit.getBlindState();
            }

            @Override
            public Future<ActionFuture> setBlindState(BlindState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.BLIND_STATE_SERVICE);
            }
        };
    }

    @Override
    public <UNIT extends StandbyStateOperationService & Unit> StandbyStateOperationService newStandbyService(final UNIT unit) {
        return new StandbyStateOperationService() {

            @Override
            public StandbyStateType.StandbyState getStandbyState() throws NotAvailableException {
                return unit.getStandbyState();
            }

            @Override
            public Future<ActionFuture> setStandbyState(StandbyStateType.StandbyState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.STANDBY_STATE_SERVICE);
            }
        };
    }

    @Override
    public <UNIT extends TargetTemperatureStateOperationService & Unit> TargetTemperatureStateOperationService newTargetTemperatureService(final UNIT unit) {
        return new TargetTemperatureStateOperationService() {

            @Override
            public TemperatureState getTargetTemperatureState() throws NotAvailableException {
                return unit.getTargetTemperatureState();
            }

            @Override
            public Future<ActionFuture> setTargetTemperatureState(TemperatureState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
            }
        };
    }
    
//    private static Future<ActionFuture> update(final Object argument, final Unit unit) throws CouldNotPerformException {
//        return update(argument, unit, Services.getServiceType(argument));
//    }

    private static Future<ActionFuture> update(final Object argument, final Unit unit, final ServiceType serviceType) throws CouldNotPerformException {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace == null) {
                throw new NotAvailableException("method stack");
            } else if (stackTrace.length == 0) {
                throw new InvalidStateException("Could not detect method stack!");
            }
            String methodName = "applyDataUpdate";
            unit.getClass().getMethod(methodName, Object.class, ServiceType.class).invoke(unit, argument, serviceType);
            return CompletableFuture.completedFuture(null);
        } catch (CouldNotPerformException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new CouldNotPerformException("Could not call remote Message[]", ex);
        }
    }
}
