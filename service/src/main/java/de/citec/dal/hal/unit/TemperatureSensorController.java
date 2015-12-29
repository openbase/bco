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
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.unit.TemperatureSensorType.TemperatureSensor;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorController extends AbstractUnitController<TemperatureSensor, TemperatureSensor.Builder> implements TemperatureSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
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

    public void updateTemperatureAlarmState(final AlarmState value) throws CouldNotPerformException {
        logger.debug("Apply alarm state Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureSensor.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setTemperatureAlarmState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not alarm state Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public AlarmState getTemperatureAlarmState() throws CouldNotPerformException {
        try {
            return getData().getTemperatureAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("temperaturealarmstate", ex);
        }
    }
}
