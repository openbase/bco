/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import rst.vision.HSVColorType;

/**
 *
 * @author mpohling
 */
public interface ColorService {

    public HSVColorType.HSVColor getColor();

    public void setColor(HSVColorType.HSVColor color) throws Exception;
}
