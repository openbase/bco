package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ButtonStateType.ButtonState;
import rst.domotic.unit.dal.ButtonDataType.ButtonData;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ButtonController extends AbstractDALUnitController<ButtonData, ButtonData.Builder> implements Button {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonState.getDefaultInstance()));
    }

    public ButtonController(final UnitHost unitHost, final ButtonData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(ButtonController.class, unitHost, builder);
    }

//    public void updateButtonStateProvider(ButtonState state) throws CouldNotPerformException {
//        logger.debug("Apply buttonState Update[" + state + "] for " + this + ".");
//        try (ClosableDataBuilder<ButtonData.Builder> dataBuilder = getDataBuilder(this)) {
//            
//            ButtonState.Builder buttonState = dataBuilder.getInternalBuilder().getButtonStateBuilder();
//
//            // Update value
//            buttonState.setValue(state.getValue());
//
//            // Update timestemp if necessary
//            if (state.getValue() == ButtonState.State.PRESSED || state.getValue() == ButtonState.State.DOUBLE_PRESSED) {
//                if (!state.hasTimestamp()) {
//                    logger.warn("State[" + state.getClass().getSimpleName() + "] of " + this + " does not contain any state related timestampe!");
//                    state = TimestampProcessor.updateTimestampWithCurrentTime(state, logger);
//                }
//                buttonState.setLastPressed(state.getTimestamp());
//            }
//            
//            dataBuilder.getInternalBuilder().setButtonState(buttonState.setTransactionId(buttonState.getTransactionId() + 1));
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not apply buttonState Update[" + state + "] for " + this + "!", ex);
//        }
//    }
//    
    @Override
    public ButtonState getButtonState() throws NotAvailableException {
        try {
            return getData().getButtonState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("buttonState state", ex);
        }
    }

    @Override
    protected void applyDataUpdate(ButtonData.Builder internalBuilder, ServiceType serviceType) {
        switch (serviceType) {
            case BUTTON_STATE_SERVICE:
                // Update timestemp if necessary
                ButtonState.Builder buttonState = internalBuilder.getButtonStateBuilder();
                if (buttonState.getValue() == ButtonState.State.PRESSED || buttonState.getValue() == ButtonState.State.DOUBLE_PRESSED) {
                    if (!buttonState.hasTimestamp()) {
                        logger.warn("State[" + buttonState.getClass().getSimpleName() + "] of " + this + " does not contain any state related timestampe!");
                        buttonState = TimestampProcessor.updateTimestampWithCurrentTime(buttonState, logger);
                    }
                    buttonState.setLastPressed(buttonState.getTimestamp());
                }
                break;
        }
    }

}
