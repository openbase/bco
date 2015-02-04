/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

/**
 *
 * @author mpohling
 */
public interface BrightnessService extends Service {

    public void setBrightness(double brightness) throws Exception;

    public double getBrightness() throws Exception;
}
