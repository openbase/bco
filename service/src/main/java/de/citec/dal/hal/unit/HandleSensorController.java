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
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rst.homeautomation.HandleSensorType;
import rst.homeautomation.HandleSensorType.HandleSensor;
import rst.homeautomation.states.OpenClosedTiltedType;
import rst.homeautomation.states.OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState;

/**
 *
 * @author thuxohl
 */
public class HandleSensorController extends AbstractUnitController<HandleSensor, HandleSensor.Builder> implements HandleSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HandleSensorType.HandleSensor.getDefaultInstance()));
    }

    public HandleSensorController(final String label, DeviceInterface hardwareUnit, HandleSensor.Builder builder) throws InstantiationException {
        super(HandleSensorController.class, label, hardwareUnit, builder);
    }

    public void updateOpenClosedTiltedState(final OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState state) {
        data.getHandleStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public OpenClosedTiltedState getRotaryHandleState() throws CouldNotPerformException{
        logger.debug("Getting [" + label + "] State: [" + data.getHandleState() + "]");
        return data.getHandleState().getState();
    }

    public class GetRotaryHandleState extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(OpenClosedTiltedState.class, HandleSensorController.this.getRotaryHandleState());
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + HandleSensorController.this.getName()+ "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }
}
