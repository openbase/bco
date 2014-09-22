/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHALController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.BrightnessSensorType;
import rst.homeautomation.BrightnessSensorType.BrightnessSensor;

/**
 *
 * @author thuxohl
 */
public class BrightnessSensorController extends AbstractHALController<BrightnessSensor, BrightnessSensor.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(BrightnessSensorType.BrightnessSensor.getDefaultInstance()));
    }
    
    public BrightnessSensorController(String id, HardwareUnit hardwareUnit, BrightnessSensor.Builder builder) throws RSBBindingException {
        super(id, hardwareUnit, builder);
    }

    public void updateBrightness(final float brightness) {
        builder.setBrightness(brightness);
        notifyChange();
    }

}
