/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractUnitController;
import org.openhab.core.library.types.DecimalType;
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
public class PowerConsumptionSensorController extends AbstractUnitController<PowerConsumptionSensor, PowerConsumptionSensor.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerConsumptionSensorType.PowerConsumptionSensor.getDefaultInstance()));
    }

    public PowerConsumptionSensorController(String id, final String lable, HardwareUnit hardwareUnit, PowerConsumptionSensor.Builder builder) throws RSBBindingException {
        super(id, lable, hardwareUnit, builder);
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("getPowerConsumption", new GetPowerConsumptionCallback());
    }

    public void updatePowerConsumption(final float consumption) {
        builder.setConsumption(consumption);
        notifyChange();
    }

    public float getPowerConsumption() {
        logger.debug("Getting [" + id + "] Consumption: [" + builder.getConsumption() + "]");
        return builder.getConsumption();
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
