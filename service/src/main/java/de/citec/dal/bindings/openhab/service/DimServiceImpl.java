/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.OpenHABCommandFactory;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.DimService;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;

/**
 *
 * @author thuxohl
 * @param <ST> Related service type.
 */
public class DimServiceImpl<ST extends DimService & Unit> extends OpenHABService<ST> implements DimService {

    public DimServiceImpl(Device device, ST unit) throws InstantiationException {
        super(device, unit);
    }

    @Override
    public void setDim(Double dimm) throws CouldNotPerformException {
        executeCommand(OpenHABCommandFactory.newPercentCommand(dimm));
    }

    @Override
    public Double getDim() throws CouldNotPerformException {
        return unit.getDim();
    }
    
}
