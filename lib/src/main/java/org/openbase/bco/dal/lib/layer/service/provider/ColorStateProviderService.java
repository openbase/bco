package org.openbase.bco.dal.lib.layer.service.provider;

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

import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.CouldNotTransformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.type.transform.HSBColorToRGBColorTransformer;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.state.PowerStateType.PowerState;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.ColorType.Color.Type;
import org.openbase.type.vision.HSBColorType.HSBColor;
import org.openbase.type.vision.RGBColorType.RGBColor;
import org.slf4j.LoggerFactory;

import static org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface ColorStateProviderService extends ProviderService {

    @RPCMethod(legacy = true)
    default ColorState getColorState() throws NotAvailableException {
        return (ColorState) getServiceProvider().getServiceState(COLOR_STATE_SERVICE);
    }

    default Color getColor() throws NotAvailableException {
        return getColorState().getColor();
    }

    default HSBColor getHSBColor() throws NotAvailableException {
        return getColorState().getColor().getHsbColor();
    }

    default RGBColor getRGBColor() throws NotAvailableException {
        return getColorState().getColor().getRgbColor();
    }

    static ColorState verifyColorState(final ColorState colorState) throws VerificationFailedException {
        if (!colorState.hasColor()) {
            throw new VerificationFailedException("Color state not available!");
        }
        return colorState.toBuilder().setColor(verifyColor(colorState.getColor())).build();
    }

    static Color verifyColor(final Color color) throws VerificationFailedException {
        final Color.Builder colorBuilder = color.toBuilder();
        if (colorBuilder.hasHsbColor()) {
            verifyHsbColor(colorBuilder.getHsbColorBuilder());
            if (!colorBuilder.hasType()) {
                colorBuilder.setType(Type.HSB).build();
            }
        } else if (colorBuilder.hasRgbColor()) {
            verifyRgbColor(colorBuilder.getRgbColorBuilder());
            if (colorBuilder.getType() == Color.Type.RGB) {
                try {
                    colorBuilder.setHsbColor(HSBColorToRGBColorTransformer.transform(colorBuilder.getRgbColor())).setType(Type.HSB).build();
                } catch (CouldNotTransformException ex) {
                    throw new VerificationFailedException("Could not transform RGB to HSV color!", ex);
                }
            }
        } else {
            throw new VerificationFailedException("Could not detect color type!");
        }
        return colorBuilder.build();
    }

    static void verifyHsbColor(final HSBColor.Builder hsbColor) throws VerificationFailedException {
        OperationService.verifyValueRange("hue", hsbColor.getHue(), 0, 360);
        hsbColor.setSaturation(ProviderService.oldValueNormalization(hsbColor.getSaturation(), 100d));
        OperationService.verifyValueRange("saturation", hsbColor.getSaturation(), 0, 1d);
        hsbColor.setBrightness(ProviderService.oldValueNormalization(hsbColor.getBrightness(), 100d));
        OperationService.verifyValueRange("brightness", hsbColor.getBrightness(), 0, 1d);
    }

    static void verifyRgbColor(final RGBColor.Builder rgbColor) throws VerificationFailedException {
        OperationService.verifyValueRange("red", rgbColor.getRed(), 0, 255);
        OperationService.verifyValueRange("green", rgbColor.getGreen(), 0, 255);
        OperationService.verifyValueRange("blue", rgbColor.getBlue(), 0, 255);
    }

    static BrightnessState toBrightnessState(final ColorState colorState) {
        HSBColor hsbColor;
        if (colorState.getColor().getType() == Type.RGB) {
            try {
                hsbColor = HSBColorToRGBColorTransformer.transform(colorState.getColor().getRgbColor());
            } catch (CouldNotTransformException ex) {
                LoggerFactory.getLogger(ColorStateProviderService.class).warn("Could not transform rgb to hsb. Continue with default value.", ex);
                hsbColor = HSBColor.getDefaultInstance();
            }
        } else {
            hsbColor = colorState.getColor().getHsbColor();
        }
        return BrightnessState.newBuilder().setBrightness(hsbColor.getBrightness()).build();
    }

    static PowerState toPowerState(final ColorState colorState) {
        return BrightnessStateProviderService.toPowerState(toBrightnessState(colorState));
    }

    static Boolean isCompatible(final ColorState colorState, final BrightnessState brightnessState) {
        try {
            // color verification is done to make sure the hsv color is at least available.
            return OperationService.equals(verifyColor(colorState.getColor()).getHsbColor().getBrightness(), brightnessState.getBrightness(), BrightnessStateProviderService.BRIGHTNESS_MARGIN);
        } catch (VerificationFailedException ex) {
            return false;
        }
    }

    static Boolean isCompatible(final ColorState colorState, final PowerState powerState) {
        System.out.println("check color comp of "+ colorState+ " and "+ powerState);
        try {
            // color verification is done to make sure the hsv color is at least available.
            switch (powerState.getValue()) {
                case ON:
                    return verifyColor(colorState.getColor()).getHsbColor().getBrightness() > 0;
                case OFF:
                    return verifyColor(colorState.getColor()).getHsbColor().getBrightness() == 0;
                case UNKNOWN:
                default:
                    return false;
            }
        } catch (VerificationFailedException ex) {
            return false;
        }
    }
}
