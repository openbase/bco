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
import rst.homeautomation.state.ClickType;
import rst.homeautomation.unit.ButtonType;
import rst.homeautomation.unit.ButtonType.Button;
import rst.homeautomation.unit.UnitConfigType;
import rst.timing.TimestampType;

/**
 *
 * @author mpohling
 */
public class ButtonController extends AbstractUnitController<Button, Button.Builder> implements ButtonInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonType.Button.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ClickType.Click.getDefaultInstance()));
    }

    public ButtonController(final UnitConfigType.UnitConfig config, Device device, Button.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, ButtonController.class, device, builder);
    }

    public void updateButton(final ClickType.Click.ClickState state) {
        data.getButtonStateBuilder().setState(state);
        if (state == ClickType.Click.ClickState.CLICKED || state == ClickType.Click.ClickState.DOUBLE_CLICKED) {
            data.getButtonStateBuilder().setLastClicked(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis()).build());
        }
        notifyChange();
    }

    @Override
    public ClickType.Click getButton() throws CouldNotPerformException {
        return data.getButtonState();
    }
}
