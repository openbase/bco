/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.PowerConsumptionSensorType;
import rst.homeautomation.unit.PowerConsumptionSensorType.PowerConsumptionSensor;

/**
 *
 * @author mpohling
 */
public class PowerConsumptionSensorController extends AbstractUnitController<PowerConsumptionSensor, PowerConsumptionSensor.Builder> implements PowerConsumptionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionSensorType.PowerConsumptionSensor.getDefaultInstance()));
    }

    public PowerConsumptionSensorController(final String label, DeviceInterface device, PowerConsumptionSensor.Builder builder) throws InstantiationException {
        super(PowerConsumptionSensorController.class, label, device, builder);
    }

    public void updatePowerConsumption(final float consumption) {
        data.setConsumption(consumption);
        notifyChange();
    }

    @Override
    public float getPowerConsumption() throws CouldNotPerformException {
        logger.debug("Getting [" + label + "] Consumption: [" + data.getConsumption() + "]");
        return data.getConsumption();
    }
}
