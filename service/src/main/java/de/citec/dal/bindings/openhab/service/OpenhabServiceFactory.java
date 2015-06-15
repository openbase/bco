/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.hal.service.BrightnessService;
import de.citec.dal.hal.service.ColorService;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.DimService;
import de.citec.dal.hal.service.OpeningRatioService;
import de.citec.dal.hal.service.ShutterService;
import de.citec.dal.hal.service.StandbyService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotSupportedException;

/**
 *
 * @author mpohling
 */
public class OpenhabServiceFactory implements ServiceFactory {

    @Override
    public BrightnessService newBrightnessService(Device device, BrightnessService unit) throws InstantiationException {
        return new BrightnessServiceImpl(device, unit);
    }

    @Override
    public ColorService newColorService(Device device, ColorService unit) throws InstantiationException {
        return new ColorServiceImpl(device, unit);
    }

    @Override
    public PowerService newPowerService(Device device, PowerService unit) throws InstantiationException {
        return new PowerServiceImpl(device, unit);
    }

    @Override
    public OpeningRatioService newOpeningRatioService(Device device, OpeningRatioService unit) throws InstantiationException {
        return new OpeningRatioServiceImpl(device, unit);
    }

    @Override
    public ShutterService newShutterService(Device device, ShutterService unit) throws InstantiationException {
        return new ShutterServiceImpl(device, unit);
    }

    @Override
    public DimService newDimmService(Device device, DimService unit) throws InstantiationException {
       return new DimServiceImpl(device, unit);
    }

    @Override
    public StandbyService newStandbyService(Device device, StandbyService unit) throws InstantiationException {
        throw new InstantiationException(this, new NotSupportedException("newStandbyService", this));
    }
}
