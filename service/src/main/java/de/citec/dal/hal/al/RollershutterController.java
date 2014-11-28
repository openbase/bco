/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.data.transform.StopMoveStateTransformer;
import de.citec.dal.data.transform.UpDownStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractUnitController;
import rsb.Event;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.RollershutterType;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.states.StopMoveType;
import rst.homeautomation.states.UpDownType;

/**
 *
 * @author thuxohl
 */
public class RollershutterController extends AbstractUnitController<RollershutterType.Rollershutter, RollershutterType.Rollershutter.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(RollershutterType.Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(StopMoveType.StopMove.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(UpDownType.UpDown.getDefaultInstance()));
    }

    public RollershutterController(String id, final String label, HardwareUnit hardwareUnit, RollershutterType.Rollershutter.Builder builder) throws RSBBindingException {
        super(id, label, hardwareUnit, builder);
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("setStopMoveState", new SetStopMoveStateCallback());
        server.addMethod("setUpDownState", new SetUpDownStateCallback());
    }

    public void updateUpDownState(final UpDownType.UpDown.UpDownState state) {
        builder.getUpDownStateBuilder().setState(state);
        notifyChange();
    }

    public void setUpDownState(final UpDownType.UpDown.UpDownState state) throws RSBBindingException {
        logger.debug("Setting [" + id + "] to UpDownState [" + state.name() + "]");
        OpenhabCommand.Builder newBuilder = OpenhabCommand.newBuilder();
        newBuilder.setUpDown(UpDownStateTransformer.transform(state)).setType(OpenhabCommand.CommandType.UPDOWN);
        executeCommand(newBuilder);
    }

    public class SetUpDownStateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                RollershutterController.this.setUpDownState(((UpDownType.UpDown) request.getData()).getState());
                return new Event(String.class, "Ok");
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setUpDownState] for [" + RollershutterController.this.getId() + "]", ex);
                return new Event(String.class, "Failed");
            }
        }
    }

    public void updateStopMoveState(final StopMoveType.StopMove.StopMoveState state) {
        builder.getStopMoveStateBuilder().setState(state);
        notifyChange();
    }

    public void setStopMoveState(final StopMoveType.StopMove.StopMoveState state) throws RSBBindingException {
        logger.debug("Setting [" + id + "] to StopMove[" + state.name() + "]");
        OpenhabCommand.Builder newBuilder = OpenhabCommand.newBuilder();
        newBuilder.setStopMove(StopMoveStateTransformer.transform(state)).setType(OpenhabCommand.CommandType.STOPMOVE);
        executeCommand(newBuilder);
    }

    public class SetStopMoveStateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                RollershutterController.this.setStopMoveState(((StopMoveType.StopMove) request.getData()).getState());
                return new Event(String.class, "Ok");
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setStopMoveState] for " + RollershutterController.this, ex);
                return new Event(String.class, "Failed");
            }
        }
    }

    public void updatePosition(final float position) {
        builder.setValue(position);
        notifyChange();
    }

    public void setPosition(final float position) throws RSBBindingException {
        logger.debug("Setting [" + id + "] to Position [" + position + "]");
        OpenhabCommand.Builder newBuilder = OpenhabCommand.newBuilder();
        newBuilder.setDecimal(rst.homeautomation.openhab.DecimalType.Decimal.newBuilder().setValue(position).build()).setType(OpenhabCommand.CommandType.DECIMAL);
        executeCommand(newBuilder);
    }

    public class SetPositionCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                RollershutterController.this.setPosition((Float) request.getData());
                return new Event(String.class, "Ok");
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setPosition] for " + RollershutterController.this, ex);
                return new Event(String.class, "Failed");
            }
        }

    }
}
