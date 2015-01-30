/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.jul.exception.TypeNotSupportedException;
import rst.vision.HSVColorType.HSVColor;
import rst.homeautomation.openhab.HSBType;

/**
 *
 * @author thuxohl
 */
public class HSVColorTransformer {

    public static HSVColor transform(HSBType.HSB color) throws RSBBindingException {
        try {
            return HSVColor.newBuilder().setHue(color.getHue()).setSaturation(color.getSaturation()).setValue(color.getBrightness()).build();
        } catch (Exception ex) {
            throw new RSBBindingException("Could not transform " + HSBType.HSB.class.getName() + " to " + HSBType.HSB.class.getName() + "!", ex);
        }
    }

    public static HSBType.HSB transform(HSVColor color) throws TypeNotSupportedException, RSBBindingException {
        try {
            return HSBType.HSB.newBuilder().setHue(color.getHue()).setSaturation(color.getSaturation()).setBrightness(color.getValue()).build();
        } catch (Exception ex) {
            throw new RSBBindingException("Could not transform " + HSVColor.class.getName() + " to " + HSBType.class.getName() + "!", ex);
        }
    }
}
