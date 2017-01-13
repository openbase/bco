package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.ButtonStateType.ButtonState;
import rst.domotic.unit.dal.ButtonDataType.ButtonData;
import rst.timing.TimestampType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ButtonController extends AbstractUnitController<ButtonData, ButtonData.Builder> implements Button {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonState.getDefaultInstance()));
    }

    public ButtonController(final UnitHost unitHost, final ButtonData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(ButtonController.class, unitHost, builder);
    }

    public void updateButtonStateProvider(final ButtonState state) throws CouldNotPerformException {

        logger.debug("Apply buttonState Update[" + state + "] for " + this + ".");

        try (ClosableDataBuilder<ButtonData.Builder> dataBuilder = getDataBuilder(this)) {

            ButtonState.Builder buttonState = dataBuilder.getInternalBuilder().getButtonStateBuilder();

            // Update value
            buttonState.setValue(state.getValue());

            // Update timestemp if necessary
            if (state.getValue() == ButtonState.State.PRESSED || state.getValue() == ButtonState.State.DOUBLE_PRESSED) {
                buttonState.setLastPressed(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis()));
            }

            dataBuilder.getInternalBuilder().setButtonState(buttonState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply buttonState Update[" + state + "] for " + this + "!", ex);
        }
    }

    @Override
    public ButtonState getButtonState() throws NotAvailableException {
        try {
            return getData().getButtonState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("buttonState state", ex);
        }
    }
}
