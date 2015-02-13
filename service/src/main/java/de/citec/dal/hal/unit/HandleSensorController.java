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
import rst.homeautomation.HandleSensorType;
import rst.homeautomation.HandleSensorType.HandleSensor;
import rst.homeautomation.states.OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState;

/**
 *
 * @author thuxohl
 */
public class HandleSensorController extends AbstractUnitController<HandleSensor, HandleSensor.Builder> implements HandleSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleSensorType.HandleSensor.getDefaultInstance()));
    }

    public HandleSensorController(final String label, DeviceInterface device, HandleSensor.Builder builder) throws InstantiationException {
        super(HandleSensorController.class, label, device, builder);
    }

    public void updateOpenClosedTiltedState(final OpenClosedTiltedState state) {
        data.getHandleStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public OpenClosedTiltedState getRotaryHandleState() throws CouldNotPerformException {
        logger.debug("Getting [" + label + "] State: [" + data.getHandleState() + "]");
        return data.getHandleState().getState();
    }
}
