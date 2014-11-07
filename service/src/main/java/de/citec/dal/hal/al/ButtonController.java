/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractUnitController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.ButtonType;
import rst.homeautomation.ButtonType.Button;
import rst.homeautomation.states.ClickType;
import rst.homeautomation.states.ClickType.Click.ClickState;

/**
 *
 * @author mpohling
 */
public class ButtonController extends AbstractUnitController<Button, Button.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(ButtonType.Button.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(ClickType.Click.getDefaultInstance()));
    }

    public ButtonController(String id, final String label, HardwareUnit hardwareUnit, Button.Builder builder) throws RSBBindingException {
        super(id, label, hardwareUnit, builder);
    }

    public void updateButtonState(final ClickType.Click.ClickState state) {
        builder.getStateBuilder().setState(state);
        notifyChange();
    }

    public ClickState getButtonState() {
        logger.debug("Getting [" + id + "] State: [" + builder.getState() + "]");
        return builder.getState().getState();
    }
}
