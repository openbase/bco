/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractUnitController;
import rsb.Event;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rst.homeautomation.TemperatureSensorType;
import rst.homeautomation.TemperatureSensorType.TemperatureSensor;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorController extends AbstractUnitController<TemperatureSensor, TemperatureSensor.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(TemperatureSensorType.TemperatureSensor.getDefaultInstance()));
    }

    public TemperatureSensorController(String id, final String label, HardwareUnit hardwareUnit, TemperatureSensor.Builder builder) throws RSBBindingException {
        super(id, label, hardwareUnit, builder);
    }

    public void updateTemperature(final float temperature) {
        builder.setTemperature(temperature);
        notifyChange();
    }

    public float getTemperature() {
        logger.debug("Getting [" + id + "] Temperature: [" + builder.getTemperature() + "]");
        return builder.getTemperature();
    }
}
