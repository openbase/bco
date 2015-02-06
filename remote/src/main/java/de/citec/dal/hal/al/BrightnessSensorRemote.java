/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.hal.unit.BrightnessSensorInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.BrightnessSensorType;

/**
 *
 * @author thuxohl
 */
public class BrightnessSensorRemote extends RSBRemoteService<BrightnessSensorType.BrightnessSensor> implements BrightnessSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessSensorType.BrightnessSensor.getDefaultInstance()));
    }

    public BrightnessSensorRemote() {
    }

    @Override
    public void notifyUpdated(BrightnessSensorType.BrightnessSensor data) {
    }

    @Override
    public double getBrightness() throws CouldNotPerformException {
        return this.getData().getBrightness();
    }
}
