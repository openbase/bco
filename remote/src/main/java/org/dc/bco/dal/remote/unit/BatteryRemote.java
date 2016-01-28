/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import org.dc.bco.dal.lib.layer.unit.BatteryInterface;
import org.dc.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.BatteryStateType.BatteryState;
import rst.homeautomation.unit.BatteryType.Battery;

/**
 *
 * @author thuxohl
 */
public class BatteryRemote extends AbstractUnitRemote<Battery> implements BatteryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Battery.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BatteryState.getDefaultInstance()));
    }

    public BatteryRemote() {
    }

    @Override
    public void notifyUpdated(Battery data) {
    }

    @Override
    public BatteryState getBattery() throws CouldNotPerformException {
        return getData().getBatteryState();
    }
}
