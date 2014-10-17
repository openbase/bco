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
import rst.homeautomation.TemperatureControllerType.TemperatureController;
import rst.homeautomation.TemperatureSensorType;
import rst.homeautomation.TemperatureSensorType.TemperatureSensor;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorController extends AbstractHALController<TemperatureSensor, TemperatureSensor.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(TemperatureSensorType.TemperatureSensor.getDefaultInstance()));
    }

    public TemperatureSensorController(String id, HardwareUnit hardwareUnit, TemperatureSensor.Builder builder) throws RSBBindingException {
        super(id, hardwareUnit, builder);
    }

    public void updateTemperature(final float temperature) {
        builder.setTemperature(temperature);
        notifyChange();
    }

    public float getTemperature() {
        logger.debug("Getting [" + id + "] Temperature: [" + builder.getTemperature() + "]");
        return builder.getTemperature();
    }

    public class GetBrightness extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(Float.class, TemperatureSensorController.this.getTemperature());
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + TemperatureSensorController.this.getId() + "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }

}
