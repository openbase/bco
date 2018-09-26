package org.openbase.bco.dal.control.layer.unit;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.Future;

import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.DimmableLight;
import org.openbase.bco.dal.lib.layer.unit.UnitHost;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.DimmableLightDataType.DimmableLightData;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE;

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

    public DimmableLightController(final UnitHost unitHost, DimmableLightData.Builder builder) throws InstantiationException {
        super(DimmableLightController.class, unitHost, builder);
    }

    @Override
    public Future<ActionFuture> setPowerState(final PowerState state) throws CouldNotPerformException {
        return applyUnauthorizedAction(state, POWER_STATE_SERVICE);
    }

    @Override
    public Future<ActionFuture> setBrightnessState(final BrightnessState state) throws CouldNotPerformException {
        return applyUnauthorizedAction(state, BRIGHTNESS_STATE_SERVICE);
    }

    @Override
    protected void applyCustomDataUpdate(DimmableLightData.Builder internalBuilder, ServiceType serviceType) {
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
