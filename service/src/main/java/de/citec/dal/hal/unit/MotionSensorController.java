/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.MotionType;
import rst.homeautomation.state.MotionType.Motion.MotionState;
import rst.homeautomation.unit.MotionSensorType;
import rst.homeautomation.unit.MotionSensorType.MotionSensor;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public class MotionSensorController extends AbstractUnitController<MotionSensor, MotionSensor.Builder> implements MotionSensorInterface{

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
    }

    public MotionSensorController(final UnitConfigType.UnitConfig config, final Device device, final MotionSensor.Builder builder) throws de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        super(config, MotionSensorController.class, device, builder);
        
    }

    public void updateMotion(final MotionType.Motion.MotionState state) {
        data.getMotionStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public MotionState getMotion() throws CouldNotPerformException{
        logger.debug("Getting [" + label + "] State: [" + data.getMotionState() + "]");
        return data.getMotionState().getState();
    }
}
