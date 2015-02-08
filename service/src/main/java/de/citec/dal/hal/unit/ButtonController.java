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
import rst.homeautomation.ButtonType;
import rst.homeautomation.ButtonType.Button;
import rst.homeautomation.states.ClickType;

/**
 *
 * @author mpohling
 */
public class ButtonController extends AbstractUnitController<Button, Button.Builder> implements ButtonInterface{

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(ButtonType.Button.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(ClickType.Click.getDefaultInstance()));
    }

    public ButtonController(String id, final String label, DeviceInterface hardwareUnit, Button.Builder builder) throws InstantiationException {
        super(ButtonController.class, label, hardwareUnit, builder);
    }

    public void updateButtonState(final ClickType.Click.ClickState state) {
        data.getButtonStateBuilder().setState(state);
        logger.debug("Updatet Button State. Sending rsb messsage...");
        notifyChange();
    }

    @Override
    public ClickType.Click.ClickState getButtonState() throws CouldNotPerformException {
        return data.getButtonState().getState();
    }
}
