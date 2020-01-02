package org.openbase.bco.app.cloudconnector.mapping.service;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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
import org.openbase.type.vision.ColorType.Color.Type;
import org.openbase.type.vision.RGBColorType.RGBColor;

/**
 * ColorTemperature was marked as deprecated by Google. Use {@link ColorStateColorSettingMapper} instead.
 */
@Deprecated
public class ColorStateColorTemperatureMapper extends AbstractServiceStateTraitMapper<ColorState> {

    public static final String MIN_KELVIN_ATTRIBUTE_KEY = "temperatureMinK";
    public static final String MAX_KELVIN_ATTRIBUTE_KEY = "temperatureMaxK";

    public static final int MIN_KELVIN_DEFAULT = 2000;
    public static final int MAX_KELVIN_DEFAULT = 6500;

    public static final String TEMPERATURE_KEY = "temperature";

    public ColorStateColorTemperatureMapper() {
        super(ServiceType.COLOR_STATE_SERVICE);
    }

    @Override
    protected ColorState map(final JsonObject jsonObject) throws CouldNotPerformException {
        if (!jsonObject.has(ColorStateColorSpectrumMapper.COLOR_KEY)) {
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + ColorState.class.getSimpleName() + "]. Attribute[" + ColorStateColorSpectrumMapper.COLOR_KEY + "] is missing");
        }

        //TODO: name for color, maybe from device class or generic -> then unit config/remote has to be given as a parameter
        try {
            final JsonObject color = jsonObject.get(ColorStateColorSpectrumMapper.COLOR_KEY).getAsJsonObject();

            if (!color.has(TEMPERATURE_KEY)) {
                throw new CouldNotPerformException("Could not map from jsonObject[" + color.toString() + "] to [" + ColorState.class.getSimpleName() + "]. Attribute[" + TEMPERATURE_KEY + "] is missing");
            }

            try {
                final int temperature = color.get(TEMPERATURE_KEY).getAsInt();
                final RGBColor rgbColor = colorTemperatureToRGB(temperature);
                final ColorState.Builder colorState = ColorState.newBuilder();
                colorState.getColorBuilder().setType(Type.RGB).setRgbColor(rgbColor);
                return colorState.build();
            } catch (ClassCastException | IllegalStateException ex) {
                // thrown if it is not an int
                throw new CouldNotPerformException("Could not map from jsonObject[" + color.toString() + "] to [" + ColorState.class.getSimpleName() + "]. Attribute[" + TEMPERATURE_KEY + "] is not an int");
            }
        } catch (ClassCastException | IllegalStateException ex) {
            // thrown if it is not a json object
            throw new CouldNotPerformException("Could not map from jsonObject[" + jsonObject.toString() + "] to [" + ColorState.class.getSimpleName() + "]. Attribute[" + ColorStateColorSpectrumMapper.COLOR_KEY + "] is not an object");
        }
    }

    @Override
    public void map(ColorState colorState, JsonObject jsonObject) throws CouldNotPerformException {
        RGBColor rgbColor;
        if (colorState.getColor().getType() == Type.HSB) {
            rgbColor = HSBColorToRGBColorTransformer.transform(colorState.getColor().getHsbColor());
        } else {
            rgbColor = colorState.getColor().getRgbColor();
        }

        JsonObject color = new JsonObject();

        //TODO: name for color
        int colorTemperature = RGBToColorTemperature(rgbColor);
        // only add property if its a valid color temperature
        if(colorTemperature >= MIN_KELVIN_DEFAULT && colorTemperature <= MAX_KELVIN_DEFAULT) {
            color.addProperty(TEMPERATURE_KEY, colorTemperature);
            jsonObject.add(ColorStateColorSpectrumMapper.COLOR_KEY, color);
        }
    }

    @Override
    public void addAttributes(UnitConfig unitConfig, JsonObject jsonObject) {
        //TODO: try to parse theses values from meta configs in unit config, device unit config, device class
        jsonObject.addProperty(MIN_KELVIN_ATTRIBUTE_KEY, MIN_KELVIN_DEFAULT);
        jsonObject.addProperty(MAX_KELVIN_ATTRIBUTE_KEY, MAX_KELVIN_DEFAULT);
    }

    /**
     * Convert color temperature given in Kelvin to RGB using
     * <a href=http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/>this algorithm</a>.
     * It is an approximation which works between 1000K and 40000K.
     *
     * @param temperature the temperature for the color in Kelvin
     * @return an rgb color type converted from the temperature
     */
    public RGBColor colorTemperatureToRGB(final int temperature) {
        double temp = temperature / 100.0;

        double red, green, blue;
        if (temp <= 66) {
            red = 255;

            green = temp;
            green = 99.4708025861 * Math.log(green) - 161.1195681661;

            if (temp <= 19) {
                blue = 0;
            } else if (temp == 66) {
                blue = 255;
            } else {
                blue = temp - 10;
                blue = 138.5177312231 * Math.log(blue) - 305.0447927307;
            }
        } else {
            red = temp - 60;
            red = 329.698727446 * Math.pow(red, -0.1332047592);

            green = temp - 60;
            green = 288.1221695283 * Math.pow(green, -0.0755148492);

            blue = 255;
        }

        red = Math.max(Math.min(red, 255), 0);
        green = Math.max(Math.min(green, 255), 0);
        blue = Math.max(Math.min(blue, 255), 0);

        RGBColor.Builder rgbColor = RGBColor.newBuilder();
        rgbColor.setRed(red / 255d).setGreen(green / 255d).setBlue(blue / 255d);
        return rgbColor.build();
    }

    /**
     * Invert the algorithm for {@link #colorTemperatureToRGB(int)}.
     * Because of rounding and double precision the inversion is most likely close
     * but not exact to the original temperature.
     *
     * @param rgbColor an rgb color type
     * @return the color temperature representing that color type
     */
    public int RGBToColorTemperature(final RGBColor rgbColor) {
        //TODO: filter green 0
        // always use green for the conversion and switch over cases using red
        if (rgbColor.getRed() == 1) {
            double res = Math.exp(((rgbColor.getGreen() * 255) + 161.1195681661) / 99.4708025861) * 100;
            return (int) res;
        } else {
            double res = (Math.exp(Math.log((rgbColor.getGreen()  * 255) / 288.1221695283) / -0.0755148492) + 60) * 100;
            return (int) res;
        }
    }
}
