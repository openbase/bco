/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.data.transform;

import de.citec.dal.exception.RSBBindingException;
import de.citec.jul.exception.TypeNotSupportedException;
import java.awt.Color;
import rst.vision.HSVColorType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author mpohling
 */
public class HSVColorToRGBColorTransformer {

    public static HSVColorType.HSVColor transform(Color color) throws RSBBindingException {
        try {
            float[] hsb = new float[3];
            Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
            return HSVColor.newBuilder().setHue(hsb[0] * 360).setSaturation(hsb[1] * 100).setValue(hsb[2] * 100).build();
        } catch (Exception ex) {
            throw new RSBBindingException("Could not transform " + Color.class.getName() + " to " + HSVColorType.HSVColor.class.getName() + "!", ex);
        }
    }
    
    public static Color transform(HSVColorType.HSVColor color) throws TypeNotSupportedException, RSBBindingException {
        try {
            return Color.getHSBColor((((float)color.getHue()) / 360f), (((float)color.getSaturation()) / 100f), (((float)color.getValue()) / 100f));
        } catch (Exception ex) {
            throw new RSBBindingException("Could not transform " + HSVColorType.HSVColor.class.getName() + " to " + Color.class.getName() + "!", ex);
        }
    }
}
