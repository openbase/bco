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
import de.citec.dal.service.rsb.RSBCommunicationService;
import org.openhab.core.library.types.DecimalType;
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

    
    // TODO thuxohl: make rsb interface more intuitive instead aligned on openhab.
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(RollershutterType.Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(ShutterType.Shutter.getDefaultInstance()));
    }

    public RollershutterController(String id, final String label, HardwareUnit hardwareUnit, RollershutterType.Rollershutter.Builder builder) throws RSBBindingException {
        super(id, label, hardwareUnit, builder);
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("setShutterState", new SetShutterStateCallback());
    }

    public void updateShutterState(final ShutterType.Shutter.ShutterState state) {
        builder.getShutterStateBuilder().setState(state);
        notifyChange();
    }

    public void setShutterState(final ShutterType.Shutter.ShutterState state) throws RSBBindingException {
        logger.debug("Setting [" + id + "] to ShutterState [" + state.name() + "]");
        executeCommand(UpDownStateTransformer.transform(state));
        executeCommand(StopMoveStateTransformer.transform(state));
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
        builder.setOpeningRatio(position);
        notifyChange();
    }

    public void setPosition(final float position) throws RSBBindingException {
        logger.debug("Setting [" + id + "] to Position [" + position + "]");
        executeCommand(new DecimalType(position));
    }

    public class SetPositionCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                RollershutterController.this.setPosition((Float) request.getData());
                return RSBCommunicationService.RPC_FEEDBACK_OK;
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setPosition] for " + RollershutterController.this, ex);
                throw ex;
            }
        }

    }
}
