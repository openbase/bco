/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.coma.dem.binding.openhab.service;

import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.coma.dem.lib.Device;
import org.dc.bco.dal.lib.layer.service.DimService;
import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.bco.dal.lib.layer.service.ShutterService;
import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotSupportedException;

/**
 *
 * @author mpohling
 */
public class OpenhabServiceFactory implements ServiceFactory {

    private final static ServiceFactory instance = new OpenhabServiceFactory();

    public static ServiceFactory getInstance() {
        return instance;
    }

    @Override
    public BrightnessService newBrightnessService(final BrightnessService unit) throws InstantiationException {
        return new BrightnessServiceImpl(device, unit);
    }

    @Override
    public ColorService newColorService(final ColorService unit) throws InstantiationException {
        return new ColorServiceImpl(device, unit);
    }

    @Override
    public PowerService newPowerService(final PowerService unit) throws InstantiationException {
        return new PowerServiceImpl(device, unit);
    }

    @Override
    public OpeningRatioService newOpeningRatioService(final OpeningRatioService unit) throws InstantiationException {
        return new OpeningRatioServiceImpl(device, unit);
    }

    @Override
    public ShutterService newShutterService(final ShutterService unit) throws InstantiationException {
        return new ShutterServiceImpl(device, unit);
    }

    @Override
    public DimService newDimmService(final DimService unit) throws InstantiationException {
       return new DimServiceImpl(device, unit);
    }

    @Override
    public StandbyService newStandbyService(final StandbyService unit) throws InstantiationException {
        throw new InstantiationException(this, new NotSupportedException("newStandbyService", this));
    }

    @Override
    public TargetTemperatureService newTargetTemperatureService(final TargetTemperatureService unit) throws InstantiationException {
        return new TargetTemperatureServiceImpl(device, unit);
    }
}
