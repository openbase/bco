/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.service;

import org.dc.bco.manager.device.binding.openhab.execution.OpenHABCommandFactory;
import org.dc.bco.manager.device.lib.Device;
import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class OpeningRatioServiceImpl<ST extends OpeningRatioService & Unit>  extends OpenHABService<ST> implements OpeningRatioService {

    public OpeningRatioServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
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
