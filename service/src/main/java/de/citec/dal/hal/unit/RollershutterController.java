/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractUnitController;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.TypeNotSupportedException;
import de.citec.jul.rsb.RSBCommunicationService;
import rsb.Event;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.RollershutterType;
import rst.homeautomation.states.ShutterType;

/**
 *
 * @author thuxohl
 */
public class RollershutterController extends AbstractUnitController<RollershutterType.Rollershutter, RollershutterType.Rollershutter.Builder> {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(RollershutterType.Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(ShutterType.Shutter.getDefaultInstance()));
    }

    public RollershutterController(String id, final String label, DeviceInterface hardwareUnit, RollershutterType.Rollershutter.Builder builder) throws InstantiationException {
        super(id, label, hardwareUnit, builder);
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("setShutterState", new SetShutterStateCallback());
    }

    public void updateShutterState(final ShutterType.Shutter.ShutterState state) {
        data.getShutterStateBuilder().setState(state);
        notifyChange();
    }

    public void setShutterState(final ShutterType.Shutter.ShutterState state) throws RSBBindingException, TypeNotSupportedException {
        logger.debug("Setting [" + id + "] to ShutterState [" + state.name() + "]");
        throw new UnsupportedOperationException("Not supported yet.");
//		OpenhabCommand.Builder newBuilder = OpenhabCommand.newBuilder();
//        newBuilder.setUpDown(UpDownStateTransformer.transform(state)).setType(OpenhabCommand.CommandType.UPDOWN);
//        executeCommand(newBuilder);
//
//		newBuilder = OpenhabCommand.newBuilder();
//		newBuilder.setStopMove(StopMoveStateTransformer.transform(state)).setType(OpenhabCommand.CommandType.STOPMOVE);
//        executeCommand(newBuilder);
    }

    public class SetShutterStateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                RollershutterController.this.setShutterState(((ShutterType.Shutter) request.getData()).getState());
                return RSBCommunicationService.RPC_FEEDBACK_OK;
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setUpDownState] for [" + RollershutterController.this.getId() + "]", ex);
                throw ex;
            }
        }
    }

    public void updatePosition(final float position) {
        data.setOpeningRatio(position);
        notifyChange();
    }

    public void setPosition(final float position) throws RSBBindingException {
        logger.debug("Setting [" + id + "] to Position [" + position + "]");
        throw new UnsupportedOperationException("Not supported yet.");
//        OpenhabCommand.Builder newBuilder = OpenhabCommand.newBuilder();
//        newBuilder.setDecimal(position).setType(OpenhabCommand.CommandType.DECIMAL);
//        executeCommand(newBuilder);
    }

//    public class SetPositionCallback extends EventCallback {
//
//        @Override
//        public Event invoke(final Event request) throws Throwable {
//            try {
//                RollershutterController.this.setPosition((Float) request.getData());
//                return RSBCommunicationService.RPC_FEEDBACK_OK;
//            } catch (Exception ex) {
//                logger.warn("Could not invoke method [setPosition] for " + RollershutterController.this, ex);
//                throw ex;
//            }
//        }
//
//    }
}
