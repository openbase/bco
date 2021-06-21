package org.openbase.bco.dal.control.layer.unit;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.DimmableLight;
import org.openbase.bco.dal.lib.layer.unit.HostUnitController;
import org.openbase.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.dal.DimmableLightDataType.DimmableLightData;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE;
import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE;

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

    public DimmableLightController(final HostUnitController hostUnitController, DimmableLightData.Builder builder) throws InstantiationException {
        super(hostUnitController, builder);
    }

    @Override
    protected void applyCustomDataUpdate(DimmableLightData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case BRIGHTNESS_STATE_SERVICE:

                updateLastWithCurrentState(POWER_STATE_SERVICE, internalBuilder);

                // sync power state
                if (internalBuilder.getBrightnessState().getBrightness() == 0d) {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.OFF);
                } else {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.ON);
                }

                copyResponsibleAction(BRIGHTNESS_STATE_SERVICE, POWER_STATE_SERVICE, internalBuilder);

                break;
            case POWER_STATE_SERVICE:

                updateLastWithCurrentState(BRIGHTNESS_STATE_SERVICE, internalBuilder);

                // sync brightness and color state.
                switch (internalBuilder.getPowerState().getValue()) {
                    case ON:
                        if (internalBuilder.getBrightnessStateBuilder().getBrightness() == 0d) {
                            internalBuilder.getBrightnessStateBuilder().setBrightness(1d);
                        }
                        break;
                    case OFF:
                        internalBuilder.getBrightnessStateBuilder().setBrightness(0d);
                        break;
                    default:
                        break;
                }

                copyResponsibleAction(POWER_STATE_SERVICE, BRIGHTNESS_STATE_SERVICE, internalBuilder);

                break;
        }
    }
}
