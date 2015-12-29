/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.homeautomation.unit.PowerConsumptionSensorType.PowerConsumptionSensor;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public class PowerConsumptionSensorController extends AbstractUnitController<PowerConsumptionSensor, PowerConsumptionSensor.Builder> implements PowerConsumptionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionState.getDefaultInstance()));
    }

    public PowerConsumptionSensorController(final UnitConfigType.UnitConfig config, Device device, PowerConsumptionSensor.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, PowerConsumptionSensorController.class, device, builder);
    }

    public void updatePowerConsumption(final PowerConsumptionState state) throws CouldNotPerformException {
        logger.debug("Apply power consumption Update[" + state + "] for " + this + ".");

        try (ClosableDataBuilder<PowerConsumptionSensor.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setPowerConsumptionState(state);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply power consumption Update[" + state + "] for " + this + "!", ex);
        }
    }

    @Override
    public PowerConsumptionState getPowerConsumption() throws NotAvailableException {
        try {
            return getData().getPowerConsumptionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("power consumption", ex);
        }
    }
}
