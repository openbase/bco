/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.hal.unit.ButtonInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.ButtonType;
import rst.homeautomation.states.ClickType;

/**
 *
 * @author thuxohl
 */
public class ButtonRemote extends RSBRemoteService<ButtonType.Button> implements ButtonInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonType.Button.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ClickType.Click.getDefaultInstance()));
    }

    public ButtonRemote() {
    }

    @Override
    public void notifyUpdated(ButtonType.Button data) {
    }

    @Override
    public ClickType.Click.ClickState getButton() throws CouldNotPerformException {
        return this.getData().getButtonState().getState();
    }
}
