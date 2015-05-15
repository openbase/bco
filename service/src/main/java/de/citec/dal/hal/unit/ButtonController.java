/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ButtonStateType.ButtonState;
import rst.homeautomation.unit.ButtonType.Button;
import rst.homeautomation.unit.UnitConfigType;
import rst.timing.TimestampType;

/**
 *
 * @author mpohling
 */
public class ButtonController extends AbstractUnitController<Button, Button.Builder> implements ButtonInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Button.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonState.getDefaultInstance()));
    }

    public ButtonController(final UnitConfigType.UnitConfig config, Device device, Button.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, ButtonController.class, device, builder);
    }

    public void updateButton(ButtonState value) throws CouldNotPerformException {

        logger.debug("Apply button Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Button.Builder> dataBuilder = getDataBuilder(this)) {

            //TODO tamino: need to be tested! Please write an unit test.
            if (value.getValue()== ButtonState.State.CLICKED || value.getValue()== ButtonState.State.DOUBLE_CLICKED) {
                value = value.toBuilder().setLastClicked(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis()).build()).build();
            }

            dataBuilder.getInternalBuilder().setButtonState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply button Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public ButtonState getButton() throws NotAvailableException {
        try {
            return getData().getButtonState();
        } catch(CouldNotPerformException ex) {
            throw new NotAvailableException("buttion state", ex);
        }
    }
}
