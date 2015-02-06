/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.AbstractUnitController;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.Event;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rst.homeautomation.MotionSensorType;
import rst.homeautomation.MotionSensorType.MotionSensor;
import rst.homeautomation.states.MotionType;
import rst.homeautomation.states.MotionType.Motion.MotionState;

/**
 *
 * @author mpohling
 */
public class MotionSensorController extends AbstractUnitController<MotionSensor, MotionSensor.Builder> implements MotionSensorInterface{

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
    }

    public MotionSensorController(final String id, final String label, final DeviceInterface hardwareUnit, final MotionSensor.Builder builder) throws de.citec.jul.exception.InstantiationException {
        super(id, label, hardwareUnit, builder);
        
    }

    public void updateMotionState(final MotionType.Motion.MotionState state) {
        data.getMotionStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public MotionState getMotionState() throws CouldNotPerformException{
        logger.debug("Getting [" + id + "] State: [" + data.getMotionState() + "]");
        return data.getMotionState().getState();
    }

    public class GetMotionState extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(MotionState.class, MotionSensorController.this.getMotionState());
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + MotionSensorController.this.getId() + "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }
}
