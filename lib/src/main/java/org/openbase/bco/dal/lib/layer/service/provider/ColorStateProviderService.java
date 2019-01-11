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

    /**
     * @return
     *
     * @throws CouldNotPerformException
     * @deprecated please use org.openbase.jul.visual.swing.transform.AWTColorToHSBColorTransformer instead.
     */
    @Deprecated
    default java.awt.Color getJavaAWTColor() throws CouldNotPerformException {
        try {
            final HSBColor color = getHSBColor();
            return java.awt.Color.getHSBColor((((float) color.getHue()) / 360f), (((float) color.getSaturation()) / 100f), (((float) color.getBrightness()) / 100f));
        } catch (Exception ex) {
            throw new CouldNotTransformException("Could not transform " + HSBColor.class.getName() + " to " + java.awt.Color.class.getName() + "!", ex);
        }
    }

    static void verifyColorState(final ColorState colorState) throws VerificationFailedException {
        if (!colorState.hasColor()) {
            throw new VerificationFailedException("Color state not available!");
        }
        verifyColor(colorState.getColor());
    }

    static Color verifyColor(final Color color) throws VerificationFailedException {
        if (color.hasHsbColor()) {
            verifyHsbColor(color.getHsbColor());
            if (!color.hasType()) {
                return color.toBuilder().setType(Type.HSB).build();
            }
        } else if (color.hasRgbColor()) {
            verifyRgbColor(color.getRgbColor());
            if (color.getType() == Color.Type.RGB) {
                try {
                    return color.toBuilder().setHsbColor(HSBColorToRGBColorTransformer.transform(color.getRgbColor())).setType(Type.HSB).build();
                } catch (CouldNotTransformException ex) {
                    throw new VerificationFailedException("Could not transform RGB to HSV color!", ex);
                }
            }
        } else {
            throw new VerificationFailedException("Could not detect color type!");
        }
        return color;
    }

    static void verifyHsbColor(final HSBColor hsbColor) throws VerificationFailedException {
        OperationService.verifyValueRange("hue", hsbColor.getHue(), 0, 360);
        OperationService.verifyValueRange("saturation", hsbColor.getSaturation(), 0, 100);
        OperationService.verifyValueRange("brightness", hsbColor.getBrightness(), 0, 100);
    }

    static void verifyRgbColor(final RGBColor rgbColor) throws VerificationFailedException {
        OperationService.verifyValueRange("red", rgbColor.getRed(), 0, 255);
        OperationService.verifyValueRange("green", rgbColor.getGreen(), 0, 255);
        OperationService.verifyValueRange("blue", rgbColor.getBlue(), 0, 255);
    }

    static BrightnessState colorStateToBrightnessState(final ColorState colorState) {
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

    static PowerState colorStateToPowerState(final ColorState colorState) {
        return BrightnessStateProviderService.brightnessStateToPowerState(colorStateToBrightnessState(colorState));
    }
}
