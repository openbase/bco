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
import rst.homeautomation.unit.PowerConsumptionSensorType;
import rst.homeautomation.unit.PowerConsumptionSensorType.PowerConsumptionSensor;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public class PowerConsumptionSensorController extends AbstractUnitController<PowerConsumptionSensor, PowerConsumptionSensor.Builder> implements PowerConsumptionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionSensorType.PowerConsumptionSensor.getDefaultInstance()));
    }

    public PowerConsumptionSensorController(final UnitConfigType.UnitConfig config, Device device, PowerConsumptionSensor.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, PowerConsumptionSensorController.class, device, builder);
    }

    public void updatePowerConsumption(final Float value) throws CouldNotPerformException {
        logger.debug("Apply power consumption Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<PowerConsumptionSensor.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setConsumption(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply power consumption Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public float getPowerConsumption() throws NotAvailableException {
        try {
            return getData().getConsumption();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("power consumption", ex);
        }
    }
}
