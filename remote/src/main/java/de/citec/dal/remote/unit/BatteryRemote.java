/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.BatteryInterface;
import de.citec.dal.hal.unit.BatteryInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.BatteryType;

/**
 *
 * @author thuxohl
 */
public class BatteryRemote extends RSBRemoteService<BatteryType.Battery> implements BatteryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BatteryType.Battery.getDefaultInstance()));
    }
    
    public BatteryRemote() {
        
    }

    @Override
    public void notifyUpdated(BatteryType.Battery data) {

    }

    @Override
    public double getBattery() throws CouldNotPerformException {
        return this.getData().getBatteryState().getLevel();
    }
}
