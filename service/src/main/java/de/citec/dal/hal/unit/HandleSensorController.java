/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState;
import rst.homeautomation.unit.HandleSensorType;
import rst.homeautomation.unit.HandleSensorType.HandleSensor;

/**
 *
 * @author thuxohl
 */
public class HandleSensorController extends AbstractUnitController<HandleSensor, HandleSensor.Builder> implements HandleSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleSensorType.HandleSensor.getDefaultInstance()));
    }

    public HandleSensorController(final String label, Device device, HandleSensor.Builder builder) throws InstantiationException {
        super(HandleSensorController.class, label, device, builder);
    }

    public void updateHandle(final OpenClosedTiltedState state) {
        data.getHandleStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public OpenClosedTiltedState getHandle() throws CouldNotPerformException {
        logger.debug("Getting [" + label + "] State: [" + data.getHandleState() + "]");
        return data.getHandleState().getState();
    }
}
