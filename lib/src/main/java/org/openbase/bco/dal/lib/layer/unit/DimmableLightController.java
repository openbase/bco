package org.openbase.bco.dal.lib.layer.unit;

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
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.schedule.FutureProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.DimmableLightDataType.DimmableLightData;
import rst.domotic.unit.UnitConfigType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmableLightController extends AbstractDALUnitController<DimmableLightData, DimmableLightData.Builder> implements DimmableLight {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DimmableLightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
    }

    private PowerStateOperationService powerService;
    private BrightnessStateOperationService brightnessService;

    public DimmableLightController(final UnitHost unitHost, DimmableLightData.Builder builder) throws org.openbase.jul.exception.InstantiationException, CouldNotPerformException {
        super(DimmableLightController.class, unitHost, builder);
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            this.powerService = getServiceFactory().newPowerService(this);
            this.brightnessService = getServiceFactory().newBrightnessService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public Future<ActionFuture> setPowerState(final PowerState state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to PowerState [" + state + "]");
        try {
            Services.verifyOperationServiceState(state);
        } catch (VerificationFailedException ex) {
            return FutureProcessor.canceledFuture(ActionFuture.class, ex);
        }
        return powerService.setPowerState(state);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("powerState", ex);
        }
    }

    @Override
    public Future<ActionFuture> setBrightnessState(final BrightnessState brightnessState) throws CouldNotPerformException {
        return brightnessService.setBrightnessState(brightnessState);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            return getData().getBrightnessState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("brightnessState", ex);
        }
    }

    @Override
    protected void applyDataUpdate(DimmableLightData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case BRIGHTNESS_STATE_SERVICE:
                if (internalBuilder.getBrightnessState().getBrightness() == 0) {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.OFF);
                } else {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.ON);
                }
                break;
        }
    }
}
