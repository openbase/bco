package org.openbase.bco.app.cloudconnector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import com.google.gson.JsonObject;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.type.transform.HSBColorToRGBColorTransformer;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.ColorType.Color.Type;
import org.openbase.type.vision.HSBColorType.HSBColor;

/**
 * Implement a mapping from Google's ColorSetting (<a href="https://developers.google.com/actions/smarthome/traits/colorsetting"></a>)
 * to BCO's ColorState.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ColorStateColorSettingMapper extends AbstractServiceStateTraitMapper<ColorState> {

    /**
     * Required if the device supports the full spectrum color model.
     * Required if the colorTemperatureRange attribute is not set.
     * Must be set to exactly one of the following values: rgb, hsv
     */
    public static final String COLOR_MODEL_ATTRIBUTE_KEY = "colorModel";
    public static final String COLOR_MODEL_HSB = "hsv";

    public static final String COLOR_KEY = "color";
    public static final String COLOR_SPECTRUM_HSV_KEY = "spectrumHSV";
    public static final String HSV_HUE_KEY = "hue";
    public static final String HSV_SATURATION_KEY = "saturation";
    public static final String HSV_VALUE_KEY = "value";

    public ColorStateColorSettingMapper() {
        super(ServiceType.COLOR_STATE_SERVICE);
    }

    @Override
    protected ColorState map(JsonObject jsonObject) throws CouldNotPerformException {
        if (!jsonObject.has(COLOR_KEY)) {
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" +
                    ColorState.class.getSimpleName() + "]. Attribute[" + COLOR_KEY + "] is missing");
        }

        // TODO: name for color
        try {
            final JsonObject colorJson = jsonObject.get(COLOR_KEY).getAsJsonObject();

            if (!colorJson.has(COLOR_SPECTRUM_HSV_KEY)) {
                throw new CouldNotPerformException("Could not map from jsonObject[" + colorJson.toString() + "] to ["
                        + ColorState.class.getSimpleName() + "]. Attribute[" + COLOR_SPECTRUM_HSV_KEY + "] is missing");
            }

            final JsonObject hsvColor = colorJson.getAsJsonObject(COLOR_SPECTRUM_HSV_KEY);
            if (!hsvColor.has(HSV_HUE_KEY) || !hsvColor.has(HSV_SATURATION_KEY) || !hsvColor.has(HSV_VALUE_KEY)) {
                throw new CouldNotPerformException("Could not map from jsonObject[" + hsvColor.toString() + "] to [" +
                        ColorState.class.getSimpleName() + "]. One of the Attributes[" +
                        HSV_HUE_KEY + ", " + HSV_SATURATION_KEY + ", " + HSV_VALUE_KEY + "] is missing");
            }
            // hue is send as a value between 0 and 360
            final double hue = hsvColor.get(HSV_HUE_KEY).getAsDouble();
            // saturation is sent as a value between 0 and 1
            final double saturation = hsvColor.get(HSV_SATURATION_KEY).getAsDouble();
            // value is sent as a value between 0 and 1
            final double value = hsvColor.get(HSV_VALUE_KEY).getAsDouble();

            // create color state from these values with type HSB
            final ColorState.Builder colorState = ColorState.newBuilder();
            final Color.Builder color = colorState.getColorBuilder();
            color.setType(Type.HSB).getHsbColorBuilder().setHue(hue).setSaturation(saturation).setBrightness(value);

            return colorState.build();
        } catch (ClassCastException | IllegalStateException ex) {
            // thrown something parsed by json has not the expected type, e.g. the value behind the color key is not
            // a jsonObject
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" +
                    ColorState.class.getSimpleName() + "]. Some attribute has not the expected type", ex);
        }
    }

    @Override
    public void map(ColorState colorState, JsonObject jsonObject) throws CouldNotPerformException {
        HSBColor hsbColor;
        switch (colorState.getColor().getType()) {
            case HSB:
                hsbColor = colorState.getColor().getHsbColor();
                break;
            case RGB:
                hsbColor = HSBColorToRGBColorTransformer.transform(colorState.getColor().getRgbColor());
                break;
            default:
                throw new CouldNotPerformException("Cannot handle transformation for type of [" + colorState.getColor() + "]");
        }

        final JsonObject color = new JsonObject();
        final JsonObject hsvColor = new JsonObject();
        hsvColor.addProperty(HSV_HUE_KEY, hsbColor.getHue());
        hsvColor.addProperty(HSV_SATURATION_KEY, hsbColor.getSaturation());
        hsvColor.addProperty(HSV_VALUE_KEY, hsbColor.getBrightness());
        color.add(COLOR_SPECTRUM_HSV_KEY, hsvColor);
        jsonObject.add(COLOR_KEY, color);
    }

    @Override
    public void addAttributes(UnitConfig unitConfig, JsonObject jsonObject) {
        // set color model to HSV/HSB
        jsonObject.addProperty(COLOR_MODEL_ATTRIBUTE_KEY, COLOR_MODEL_HSB);
    }
}
