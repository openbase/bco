/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.unit.MotionSensorType.MotionSensor;
import rst.homeautomation.unit.UnitConfigType;
import rst.timing.TimestampType;

/**
 *
 * @author mpohling
 */
public class MotionSensorController extends AbstractUnitController<MotionSensor, MotionSensor.Builder> implements MotionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionState.getDefaultInstance()));
    }

    public MotionSensorController(final UnitConfigType.UnitConfig config, final Device device, final MotionSensor.Builder builder) throws de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        super(config, MotionSensorController.class, device, builder);

    }

    public void updateMotion(MotionState value) throws CouldNotPerformException {
        logger.debug("Apply motion Update[" + value + "] for " + this + ".");
        try (ClosableDataBuilder<MotionSensor.Builder> dataBuilder = getDataBuilder(this)) {

            //TODO tamino: need to be tested! Please write an unit test.
            if (value.getValue()== MotionState.State.MOVEMENT) {
                value = value.toBuilder().setLastMovement(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis())).build();
            }

            dataBuilder.getInternalBuilder().setMotionState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply motion Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public MotionState getMotion() throws NotAvailableException {
        try {
            return getData().getMotionState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("motion", ex);
        }
    }
}
