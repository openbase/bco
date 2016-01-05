/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service;

import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.InstantiationException;

/**
 *
 * @author mpohling
 */
public interface ServiceFactory {

    public abstract <UNIT extends BrightnessService & Unit> BrightnessService newBrightnessService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends ColorService & Unit> ColorService newColorService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends PowerService & Unit> PowerService newPowerService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends OpeningRatioService & Unit> OpeningRatioService newOpeningRatioService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends ShutterService & Unit> ShutterService newShutterService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends DimService & Unit> DimService newDimmService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends StandbyService & Unit> StandbyService newStandbyService(UNIT unit) throws InstantiationException;

    public abstract <UNIT extends TargetTemperatureService & Unit> TargetTemperatureService newTargetTemperatureService(UNIT unit) throws InstantiationException;

}
