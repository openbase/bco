/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.OpeningRationProvider;
import de.citec.jul.exception.CouldNotPerformException;

/**
 *
 * @author thuxohl
 */
public interface OpeningRatioService extends Service, OpeningRationProvider{
    
    public void setOpeningRatio(double openingRatio) throws CouldNotPerformException;
}
