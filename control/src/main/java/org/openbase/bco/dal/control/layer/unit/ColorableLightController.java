package org.openbase.bco.dal.control.layer.unit;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.ColorableLight;
import org.openbase.bco.dal.lib.layer.unit.HostUnitController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.schedule.CloseableWriteLockWrapper;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType.ColorableLightData;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.openbase.type.vision.RGBColorType.RGBColor;

import java.util.concurrent.Future;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.*;

/**
 * @author Tamino Huxohl
 * @author Marian Pohling
 */
public class ColorableLightController extends AbstractDALUnitController<ColorableLightData, ColorableLightData.Builder> implements ColorableLight {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorableLightData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RGBColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Color.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ColorState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
    }

    private Color neutralWhiteColor;

    public ColorableLightController(final HostUnitController hostUnitController, final ColorableLightData.Builder builder) throws InstantiationException {
        super(hostUnitController, builder);
        this.neutralWhiteColor = ColorableLight.DEFAULT_NEUTRAL_WHITE_COLOR;
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        try (final CloseableWriteLockWrapper ignored = getManageWriteLockInterruptible(this)) {
            neutralWhiteColor = ColorableLight.detectNeutralWhiteColor(config, logger);
            return super.applyConfigUpdate(config);
        }
    }

    @Override
    public Future<ActionDescription> setNeutralWhite() {
        return setColor(neutralWhiteColor);
    }

    @Override
    protected void applyCustomDataUpdate(ColorableLightData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case COLOR_STATE_SERVICE:

                updateLastWithCurrentState(BRIGHTNESS_STATE_SERVICE, internalBuilder);
                updateLastWithCurrentState(POWER_STATE_SERVICE, internalBuilder);

                final HSBColor hsbColor = internalBuilder.getColorState().getColor().getHsbColor();
                internalBuilder.getBrightnessStateBuilder().setBrightness(hsbColor.getBrightness());

                if (hsbColor.getBrightness() == 0d) {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.OFF);
                } else {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.ON);
                }

                copyResponsibleAction(COLOR_STATE_SERVICE, BRIGHTNESS_STATE_SERVICE, internalBuilder);
                copyResponsibleAction(COLOR_STATE_SERVICE, POWER_STATE_SERVICE, internalBuilder);

                break;
            case BRIGHTNESS_STATE_SERVICE:

                updateLastWithCurrentState(COLOR_STATE_SERVICE, internalBuilder);
                updateLastWithCurrentState(POWER_STATE_SERVICE, internalBuilder);

                // sync color brightness state.
                internalBuilder.getColorStateBuilder().getColorBuilder().getHsbColorBuilder().setBrightness(internalBuilder.getBrightnessState().getBrightness());

                // sync power state
                if (internalBuilder.getBrightnessState().getBrightness() == 0d) {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.OFF);
                } else {
                    internalBuilder.getPowerStateBuilder().setValue(PowerState.State.ON);
                }

                copyResponsibleAction(BRIGHTNESS_STATE_SERVICE, COLOR_STATE_SERVICE, internalBuilder);
                copyResponsibleAction(BRIGHTNESS_STATE_SERVICE, POWER_STATE_SERVICE, internalBuilder);

                break;
            case POWER_STATE_SERVICE:

                updateLastWithCurrentState(COLOR_STATE_SERVICE, internalBuilder);
                updateLastWithCurrentState(BRIGHTNESS_STATE_SERVICE, internalBuilder);

                // sync brightness and color state.
                switch (internalBuilder.getPowerState().getValue()) {
                    case ON:
                        if (internalBuilder.getBrightnessStateBuilder().getBrightness() == 0d) {
                            internalBuilder.getBrightnessStateBuilder().setBrightness(1d);
                        }
                        if (internalBuilder.getColorStateBuilder().getColorBuilder().getHsbColorBuilder().getBrightness() == 0d) {
                            internalBuilder.getColorStateBuilder().getColorBuilder().getHsbColorBuilder().setBrightness(1d);
                        }
                        break;
                    case OFF:
                        internalBuilder.getBrightnessStateBuilder().setBrightness(0d);
                        internalBuilder.getColorStateBuilder().getColorBuilder().getHsbColorBuilder().setBrightness(0d);
                        break;
                    default:
                        break;
                }

                copyResponsibleAction(POWER_STATE_SERVICE, COLOR_STATE_SERVICE, internalBuilder);
                copyResponsibleAction(POWER_STATE_SERVICE, BRIGHTNESS_STATE_SERVICE, internalBuilder);

                break;
        }
    }

    @Override
    public Color getNeutralWhiteColor() {
        return neutralWhiteColor;
    }
}
