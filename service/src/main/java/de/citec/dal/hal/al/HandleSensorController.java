/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHALController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.HandleSensorType;
import rst.homeautomation.HandleSensorType.HandleSensor;
import rst.homeautomation.states.OpenClosedTiltedType;

/**
 *
 * @author thuxohl
 */
public class HandleSensorController extends AbstractHALController<HandleSensor, HandleSensor.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HandleSensorType.HandleSensor.getDefaultInstance()));
    }
    
    public HandleSensorController(String id, HardwareUnit hardwareUnit, HandleSensor.Builder builder) throws RSBBindingException {
        super(id, hardwareUnit, builder);
    }

    public void updateOpenClosedTiltedState(final OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState state) {
        builder.getStateBuilder().setState(state);
        notifyChange();
    }

}
