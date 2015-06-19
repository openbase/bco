/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.ClosableDataBuilder;
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

    public void updateTemperature(final Double value) throws CouldNotPerformException {
        logger.debug("Apply temperature Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureSensor.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setTemperature(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply temperature Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Double getTemperature() throws NotAvailableException {
        try {
            return getData().getTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("temperature", ex);
        }
    }
}
