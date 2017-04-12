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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.slf4j.LoggerFactory;
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

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ServiceFactoryMock.class);

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
            public Future<Void> setBrightnessState(BrightnessState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit);
            }
        };
    }

    @Override
    public <UNIT extends ColorStateOperationService & Unit> ColorStateOperationService newColorService(final UNIT unit) throws org.openbase.jul.exception.InstantiationException {
//        ServiceSimulator serviceSimulator = new ServiceSimulator(ServiceType.COLOR_STATE_SERVICE, unit, 500);
//        serviceSimulator.addState(ColorState.newBuilder().setColor(Color.newBuilder().setHsbColor(HSBColorType.HSBColor.newBuilder()
//                .setBrightness(100)
//                .setSaturation(100)
//                .setHue(100).build()).build()).build());
//        serviceSimulator.addState(ColorState.newBuilder().setColor(Color.newBuilder().setHsbColor(HSBColorType.HSBColor.newBuilder()
//                .setBrightness(100)
//                .setSaturation(100)
//                .setHue(200).build()).build()).build());
//        serviceSimulator.addState(ColorState.newBuilder().setColor(Color.newBuilder().setHsbColor(HSBColorType.HSBColor.newBuilder()
//                .setBrightness(100)
//                .setSaturation(100)
//                .setHue(300).build()).build()).build());
//        serviceSimulator.addState(ColorState.newBuilder().setColor(Color.newBuilder().setHsbColor(HSBColorType.HSBColor.newBuilder()
//                .setBrightness(100)
//                .setSaturation(100)
//                .setHue(0).build()).build()).build());
//        serviceSimulator.addState(ColorState.newBuilder().setColor(Color.newBuilder().setHsbColor(HSBColorType.HSBColor.newBuilder()
//                .setBrightness(0)
//                .setSaturation(100)
//                .setHue(0).build()).build()).build());
//        serviceSimulator.addState(ColorState.newBuilder().setColor(Color.newBuilder().setHsbColor(HSBColorType.HSBColor.newBuilder()
//                .setBrightness(100)
//                .setSaturation(0)
//                .setHue(0).build()).build()).build());

        return new ColorStateOperationService() {

            @Override
            public ColorState getColorState() throws NotAvailableException {
                return ((ColorStateOperationService) unit).getColorState();
            }

            @Override
            public Future<Void> setColorState(ColorState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit);
            }
        };
    }

    @Override
    public <UNIT extends PowerStateOperationService & Unit> PowerStateOperationService newPowerService(final UNIT unit) throws org.openbase.jul.exception.InstantiationException {

//        ServiceSimulator serviceSimulator = new ServiceSimulator(ServiceType.POWER_STATE_SERVICE, unit, 500);
//        serviceSimulator.addState(PowerState.newBuilder().setValue(PowerState.State.OFF).build());
//        serviceSimulator.addState(PowerState.newBuilder().setValue(PowerState.State.ON).build());
        return new PowerStateOperationService() {

            @Override
            public PowerState getPowerState() throws NotAvailableException {
                return ((PowerStateOperationService) unit).getPowerState();
            }

            @Override
            public Future<Void> setPowerState(PowerState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit);
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
            public Future<Void> setBlindState(BlindState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit);
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
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit);
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
            public Future<Void> setTargetTemperatureState(TemperatureState state) throws CouldNotPerformException {
                return update(TimestampProcessor.updateTimestampWithCurrentTime(state), unit);
            }
        };
    }

    private static Future<Void> update(final Object argument, final Unit unit) throws CouldNotPerformException {
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

    private static class ServiceSimulator<SERVICE_STATE> {

        private List<SERVICE_STATE> stateList;
        private Runnable modificationCommand;
        private static Random random = new Random();

        public ServiceSimulator(final ServiceType serviceType, final Unit unit, final long changeRate) throws InstantiationException {
            try {
                this.stateList = new ArrayList<>();
                this.modificationCommand = new Runnable() {
                    @Override
                    public void run() {
                        // skip if no values are defined.
                        if (stateList.isEmpty()) {
                            return;
                        }

                        // apply random service manipulation
                        try {
                            Service.invokeOperationServiceMethod(serviceType, unit, stateList.get(random.nextInt(stateList.size())));
                        } catch (CouldNotPerformException ex) {
                            ExceptionPrinter.printHistory("Could not apply service modification!", ex, logger);
                        }
                    }
                };
                GlobalScheduledExecutorService.scheduleAtFixedRate(modificationCommand, random.nextInt(30000) + 30000, changeRate, TimeUnit.MILLISECONDS);
            } catch (CouldNotPerformException ex) {
                throw new InstantiationException(this, ex);
            }
        }

        public void addState(final SERVICE_STATE state) {
            stateList.add(state);
        }
    }
}
