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
import rst.homeautomation.ReedSwitchType;
import rst.homeautomation.ReedSwitchType.ReedSwitch;
import rst.homeautomation.states.OpenClosedType;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchController extends AbstractHALController<ReedSwitch, ReedSwitch.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(ReedSwitchType.ReedSwitch.getDefaultInstance()));
    }
    
    public ReedSwitchController(String id, HardwareUnit hardwareUnit, ReedSwitch.Builder builder) throws RSBBindingException {
        super(id, hardwareUnit, builder);
    }

    public void updateOpenClosedState(final OpenClosedType.OpenClosed.OpenClosedState state) {
        builder.getStateBuilder().setState(state);
        notifyChange();
    }

}
