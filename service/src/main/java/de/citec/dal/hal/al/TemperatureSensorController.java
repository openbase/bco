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

}
