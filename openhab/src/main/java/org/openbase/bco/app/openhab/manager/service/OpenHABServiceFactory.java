package org.openbase.bco.app.openhab.manager.service;

import org.openbase.bco.dal.lib.layer.service.ServiceFactory;
import org.openbase.bco.dal.lib.layer.service.operation.*;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.InstantiationException;

public class OpenHABServiceFactory implements ServiceFactory {

    private final static ServiceFactory instance = new OpenHABServiceFactory();

    public static ServiceFactory getInstance() {
        return instance;
    }

    @Override
    public <UNIT extends BrightnessStateOperationService & Unit> BrightnessStateOperationService newBrightnessService(UNIT unit) throws InstantiationException {
        return new BrightnessStateServiceImpl<>(unit);
    }

    @Override
    public <UNIT extends ColorStateOperationService & Unit> ColorStateOperationService newColorService(final UNIT unit) throws InstantiationException {
        return new ColorStateServiceImpl(unit);
    }

    @Override
    public <UNIT extends PowerStateOperationService & Unit> PowerStateOperationService newPowerService(final UNIT unit) throws InstantiationException {
        return new PowerStateServiceImpl(unit);
    }

    @Override
    public <UNIT extends BlindStateOperationService & Unit> BlindStateOperationService newShutterService(final UNIT unit) throws InstantiationException {
        return new BlindStateServiceImpl<>(unit);
    }

    @Override
    public <UNIT extends StandbyStateOperationService & Unit> StandbyStateOperationService newStandbyService(final UNIT unit) throws InstantiationException {
        return new StandbyStateServiceImpl<>(unit);
    }

    @Override
    public <UNIT extends TargetTemperatureStateOperationService & Unit> TargetTemperatureStateOperationService newTargetTemperatureService(final UNIT unit) throws InstantiationException {
        return new TargetTemperatureStateServiceImpl<>(unit);
    }

}
