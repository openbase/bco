/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import org.dc.bco.dal.lib.layer.unit.BrightnessSensorInterface;
import org.dc.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.BrightnessSensorType;

/**
 *
 * @author thuxohl
 */
public class BrightnessSensorRemote extends AbstractUnitRemote<BrightnessSensorType.BrightnessSensor> implements BrightnessSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessSensorType.BrightnessSensor.getDefaultInstance()));
    }

    public BrightnessSensorRemote() {
    }

    @Override
    public void notifyUpdated(BrightnessSensorType.BrightnessSensor data) {
    }

    @Override
    public Double getBrightness() throws CouldNotPerformException {
        return getData().getBrightness();
    }
}
