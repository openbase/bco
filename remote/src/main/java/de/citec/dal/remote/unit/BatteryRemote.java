/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.BatteryInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.BatteryType.Battery;
import rst.homeautomation.state.BatteryStateType.BatteryState;

/**
 *
 * @author thuxohl
 */
public class BatteryRemote extends DALRemoteService<Battery> implements BatteryInterface {

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
