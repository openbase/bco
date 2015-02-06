/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.Event;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.PowerConsumptionSensorType;
import rst.homeautomation.PowerConsumptionSensorType.PowerConsumptionSensor;

/**
 *
 * @author mpohling
 */
public class PowerConsumptionSensorController extends AbstractUnitController<PowerConsumptionSensor, PowerConsumptionSensor.Builder> implements PowerConsumptionSensorInterface{

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerConsumptionSensorType.PowerConsumptionSensor.getDefaultInstance()));
    }

    public PowerConsumptionSensorController(String id, final String label, DeviceInterface hardwareUnit, PowerConsumptionSensor.Builder builder) throws InstantiationException {
        super(id, label, hardwareUnit, builder);
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("getPowerConsumption", new GetPowerConsumptionCallback());
    }

    public void updatePowerConsumption(final float consumption) {
        data.setConsumption(consumption);
        notifyChange();
    }

    @Override
    public float getPowerConsumption() throws CouldNotPerformException {
        logger.debug("Getting [" + id + "] Consumption: [" + data.getConsumption() + "]");
        return data.getConsumption();
    }

    public class GetPowerConsumptionCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(Float.class, PowerConsumptionSensorController.this.getPowerConsumption());
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + PowerConsumptionSensorController.this.getId() + "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }
}
