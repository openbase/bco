/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.TemperatureSensorType;
import rst.homeautomation.unit.TemperatureSensorType.TemperatureSensor;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorController extends AbstractUnitController<TemperatureSensor, TemperatureSensor.Builder> implements TemperatureSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureSensorType.TemperatureSensor.getDefaultInstance()));
    }

    public TemperatureSensorController(final UnitConfigType.UnitConfig config, Device device, TemperatureSensor.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, TemperatureSensorController.class, device, builder);
    }

    public void updateTemperature(final float temperature) {
        data.setTemperature(temperature);
        notifyChange();
    }

    @Override
    public float getTemperature() throws CouldNotPerformException {
        logger.debug("Getting [" + getLabel() + "] Temperature: [" + data.getTemperature() + "]");
        return data.getTemperature();
    }
}
