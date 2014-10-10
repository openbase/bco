/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHALController;
import rsb.Event;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
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

    public float getBrightness() {
        logger.debug("Getting [" + id + "] Brightness: [" + builder.getBrightness() + "]");
        return builder.getBrightness();
    }

    public class GetBrightness extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(Float.class, BrightnessSensorController.this.getBrightness());
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + BrightnessSensorController.this.getId() + "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }
}
