package org.openbase.bco.dal.lib.layer.service.mock;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceProvider;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.*;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.vision.ColorType.Color;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.state.StandbyStateType;
import org.openbase.type.domotic.state.TemperatureStateType.TemperatureState;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class OperationServiceFactoryMock implements OperationServiceFactory {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OperationServiceFactoryMock.class);

    private static OperationServiceFactory instance = new OperationServiceFactoryMock();

    public static OperationServiceFactory getInstance() {
        if (instance == null) {
            instance = new OperationServiceFactoryMock();
        }
        return instance;
    }

    private OperationServiceFactoryMock() {
    }

    @Override
    public <UNIT extends UnitController<?, ?>> OperationService newInstance(final ServiceType operationServiceType, final UNIT unit) throws InstantiationException {
        try {
            final Class<?> operationServiceClass = Services.loadOperationServiceClass(operationServiceType);
            String mockClassName = StringProcessor.transformUpperCaseToPascalCase(operationServiceType.name());
            mockClassName = mockClassName.replace(Service.class.getSimpleName(), OperationService.class.getSimpleName());
            mockClassName += "Mock";
            Constructor<?> constructor = getClass().getClassLoader().loadClass(getClass().getName() + "$" + mockClassName).getConstructor(operationServiceClass);
            return (OperationService) constructor.newInstance(unit);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | java.lang.InstantiationException ex) {
            throw new InstantiationException(OperationService.class, operationServiceType.name(), ex);
        }
    }

    public static class BrightnessStateOperationServiceMock<UNIT extends BrightnessStateOperationService & UnitController<?, ?>> implements BrightnessStateOperationService {

        private final UNIT unit;

        public BrightnessStateOperationServiceMock(final UNIT unit) {
            this.unit = unit;
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return unit;
        }

        @Override
        public BrightnessState getBrightnessState() throws NotAvailableException {
            return unit.getBrightnessState();
        }

        @Override
        public Future<ActionDescription> setBrightnessState(BrightnessState state) {
            try {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.BRIGHTNESS_STATE_SERVICE);
            } catch (CouldNotPerformException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, ex);
            }
        }
    }

    public static class ColorStateOperationServiceMock<UNIT extends ColorStateOperationService & UnitController<?, ?>> implements ColorStateOperationService {

        private final UNIT unit;

        public ColorStateOperationServiceMock(final UNIT unit) {
            this.unit = unit;
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return unit;
        }

        @Override
        public ColorState getColorState() throws NotAvailableException {
            return unit.getColorState();
        }

        @Override
        public Future<ActionDescription> setColorState(ColorState state) {
            try {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.COLOR_STATE_SERVICE);
            } catch (CouldNotPerformException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, ex);
            }
        }

        @Override
        public Color getNeutralWhiteColor() throws NotAvailableException {
            return unit.getNeutralWhiteColor();
        }
    }

    public static class PowerStateOperationServiceMock<UNIT extends PowerStateOperationService & UnitController<?, ?>> implements PowerStateOperationService {

        final UNIT unit;

        public PowerStateOperationServiceMock(final UNIT unit) {
            this.unit = unit;
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return unit;
        }

        @Override
        public PowerState getPowerState() throws NotAvailableException {
            return unit.getPowerState();
        }

        @Override
        public Future<ActionDescription> setPowerState(PowerState state) {
            try {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.POWER_STATE_SERVICE);
            } catch (CouldNotPerformException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, ex);
            }
        }
    }

    public static class BlindStateOperationServiceMock<UNIT extends BlindStateOperationService & UnitController<?, ?>> implements BlindStateOperationService {

        final UNIT unit;

        public BlindStateOperationServiceMock(final UNIT unit) {
            this.unit = unit;
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return unit;
        }

        @Override
        public BlindState getBlindState() throws NotAvailableException {
            return unit.getBlindState();
        }

        @Override
        public Future<ActionDescription> setBlindState(BlindState state) {
            try {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.BLIND_STATE_SERVICE);
            } catch (CouldNotPerformException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, ex);
            }
        }
    }

    public static class StandbyStateOperationServiceMock<UNIT extends StandbyStateOperationService & UnitController<?, ?>> implements StandbyStateOperationService {

        final UNIT unit;

        public StandbyStateOperationServiceMock(final UNIT unit) {
            this.unit = unit;
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return unit;
        }

        @Override
        public StandbyStateType.StandbyState getStandbyState() throws NotAvailableException {
            return unit.getStandbyState();
        }

        @Override
        public Future<ActionDescription> setStandbyState(StandbyStateType.StandbyState state) {
            try {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.STANDBY_STATE_SERVICE);
            } catch (CouldNotPerformException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, ex);
            }
        }
    }

    public static class TargetTemperatureStateOperationServiceMock<UNIT extends TargetTemperatureStateOperationService & UnitController<?, ?>> implements TargetTemperatureStateOperationService {

        final UNIT unit;

        public TargetTemperatureStateOperationServiceMock(final UNIT unit) {
            this.unit = unit;
        }

        @Override
        public ServiceProvider getServiceProvider() {
            return unit;
        }

        @Override
        public TemperatureState getTargetTemperatureState() throws NotAvailableException {
            return unit.getTargetTemperatureState();
        }

        @Override
        public Future<ActionDescription> setTargetTemperatureState(TemperatureState state) {
            try {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit, ServiceType.TARGET_TEMPERATURE_STATE_SERVICE);
            } catch (CouldNotPerformException ex) {
                return FutureProcessor.canceledFuture(ActionDescription.class, ex);
            }
        }
    }

    private static Future<ActionDescription> update(final Message argument, final UnitController<?, ?> unit, final ServiceType serviceType) {
        try {
            unit.applyServiceState(argument, serviceType);
            return FutureProcessor.completedFuture(null);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, new CouldNotPerformException("Could not call remote Message[]", ex));
        }
    }
}
