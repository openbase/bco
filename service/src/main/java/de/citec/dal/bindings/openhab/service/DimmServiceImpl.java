/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.DimmService;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class DimmServiceImpl<ST extends DimmService & Unit> extends OpenHABService<ST> implements DimmService {

    public DimmServiceImpl(Device device, ST unit) throws InstantiationException {
        super(device, unit);
    }

    @Override
    public void setDimm(Double dimm) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newPercentCommand(dimm));
    }

    @Override
    public double getDimm() throws CouldNotPerformException {
        return unit.getDimm();
    }
    
}
