/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.TargetTemperatureService;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class TargetTemperatureServiceImpl<ST extends TargetTemperatureService & Unit> extends OpenHABService<ST> implements TargetTemperatureService {

    public TargetTemperatureServiceImpl(Device device, ST unit) throws InstantiationException {
        super(device, unit);
    }
    
    @Override
    public void setTargetTemperature(Double value) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newDecimalCommand(value));
    }

    @Override
    public Double getTargetTemperature() throws CouldNotPerformException {
        return unit.getTargetTemperature();
    }

}
