/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.exception.DALException;
import de.citec.dal.hal.AbstractUnitController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.TemperatureSensorType;
import rst.homeautomation.TemperatureSensorType.TemperatureSensor;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorController extends AbstractUnitController<TemperatureSensor, TemperatureSensor.Builder> implements TemperatureSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(TemperatureSensorType.TemperatureSensor.getDefaultInstance()));
    }

    public TemperatureSensorController(String id, final String label, DeviceInterface hardwareUnit, TemperatureSensor.Builder builder) throws InstantiationException {
        super(id, label, hardwareUnit, builder);
    }

    public void updateTemperature(final float temperature) {
        data.setTemperature(temperature);
        notifyChange();
    }

    @Override
    public float getTemperature() throws CouldNotPerformException {
        logger.debug("Getting [" + id + "] Temperature: [" + data.getTemperature() + "]");
        return data.getTemperature();
    }
}
