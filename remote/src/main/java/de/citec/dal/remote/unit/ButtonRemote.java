/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.ButtonInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ButtonStateType.ButtonState;
import rst.homeautomation.unit.ButtonType.Button;

/**
 *
 * @author thuxohl
 */
public class ButtonRemote extends DALRemoteService<Button> implements ButtonInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Button.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonState.getDefaultInstance()));
    }

    public ButtonRemote() {
    }

    @Override
    public void notifyUpdated(Button data) {
    }

    @Override
    public ButtonState getButton() throws CouldNotPerformException {
        return this.getData().getButtonState();
    }
}
