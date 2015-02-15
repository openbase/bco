/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.service.OpeningRatioService;
import de.citec.dal.hal.unit.UnitInterface;
import de.citec.jul.exception.CouldNotPerformException;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class OpeningRatioServiceImpl<ST extends OpeningRatioService & UnitInterface>  extends OpenHABService<ST> implements OpeningRatioService {

    public OpeningRatioServiceImpl(DeviceInterface device, ST unit) {
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
