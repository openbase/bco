/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import java.util.logging.Level;
import java.util.logging.Logger;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.MotionType;
import rst.homeautomation.state.MotionType.Motion.MotionState;
import rst.homeautomation.unit.MotionSensorType;
import rst.homeautomation.unit.MotionSensorType.MotionSensor;
import rst.homeautomation.unit.UnitConfigType;
import rst.timing.TimestampType;

/**
 *
 * @author mpohling
 */
public class MotionSensorController extends AbstractUnitController<MotionSensor, MotionSensor.Builder> implements MotionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
    }

    public MotionSensorController(final UnitConfigType.UnitConfig config, final Device device, final MotionSensor.Builder builder) throws de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        super(config, MotionSensorController.class, device, builder);

    }

    public void updateMotion(MotionType.Motion motion) throws CouldNotPerformException {
        //TODO tamino: need to be tested! Please write an unit test.
        if (motion.getState() == MotionState.MOVEMENT) {
            motion = motion.toBuilder().setLastMovement(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis())).build();
        }

        try (ClosableDataBuilder builder = getClosableDataBuilder()) {
            builder.builder.setMotionState(motion);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply data change!", ex);
        }

        notifyChange();
    }

    @Override
    public MotionType.Motion getMotion() throws CouldNotPerformException {
        try {
            MotionType.Motion motionState = getData().getMotionState();
            logger.debug("Getting [" + getLabel() + "] State: [" + motionState + "]");
            return motionState;
        } catch (RSBException ex) {
            throw new CouldNotPerformException("Could not return motion data.", ex);
        }
    }
}
