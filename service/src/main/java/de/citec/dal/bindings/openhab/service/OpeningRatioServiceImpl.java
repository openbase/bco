/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.OpeningRatioService;
import de.citec.dal.hal.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class OpeningRatioServiceImpl<ST extends OpeningRatioService & Unit>  extends OpenHABService<ST> implements OpeningRatioService {

    public OpeningRatioServiceImpl(Device device, ST unit) throws InstantiationException {
        super(device, unit);
    }

    @Override
    public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newPercentCommand(openingRatio));
    }

    @Override
    public Double getOpeningRatio() throws CouldNotPerformException {
        return unit.getOpeningRatio();
    }
}
