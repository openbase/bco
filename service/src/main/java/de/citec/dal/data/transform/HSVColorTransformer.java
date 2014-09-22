/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.TypeNotSupportedException;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author thuxohl
 */
public class HSVColorTransformer {

    public static HSVColor transform(HSBType color) throws RSBBindingException {
        try {
            return HSVColor.newBuilder().setHue(color.getHue().doubleValue()).setSaturation(color.getSaturation().doubleValue()).setValue(color.getBrightness().doubleValue()).build();
        } catch (Exception ex) {
            throw new RSBBindingException("Could not transform " + HSBType.class.getName() + " to " + HSVColor.class.getName() + "!", ex);
        }
    }

    public static HSBType transform(HSVColor color) throws TypeNotSupportedException, RSBBindingException {
        try {
            return new HSBType(new DecimalType(color.getHue()), new PercentType((int) (color.getSaturation())), new PercentType((int) (color.getValue())));
        } catch (Exception ex) {
            throw new RSBBindingException("Could not transform " + HSVColor.class.getName() + " to " + HSBType.class.getName() + "!", ex);
        }
    }
}
