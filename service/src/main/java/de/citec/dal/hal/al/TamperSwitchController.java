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
import rst.homeautomation.TamperSwitchType;
import rst.homeautomation.TamperSwitchType.TamperSwitch;
import rst.homeautomation.states.TamperType;

/**
 *
 * @author thuxohl
 */
public class TamperSwitchController extends AbstractHALController<TamperSwitch, TamperSwitch.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(TamperSwitchType.TamperSwitch.getDefaultInstance()));
    }
    
    public TamperSwitchController(String id, HardwareUnit hardwareUnit, TamperSwitch.Builder builder) throws RSBBindingException {
        super(id, hardwareUnit, builder);
    }

    public void updateTamperState(final TamperType.Tamper.TamperState state) {
        builder.getStateBuilder().setState(state);
        notifyChange();
    }

}
