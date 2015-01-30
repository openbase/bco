/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.data.transform.PowerStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractUnitController;
import rsb.Event;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.PowerPlugType;
import rst.homeautomation.PowerPlugType.PowerPlug;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author mpohling
 */
public class PowerPlugController extends AbstractUnitController<PowerPlug, PowerPlug.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerPlugType.PowerPlug.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    public PowerPlugController(String id, final String label, HardwareUnit hardwareUnit, PowerPlug.Builder builder) throws RSBBindingException {
        super(id, label, hardwareUnit, builder);
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("setPowerState", new SetPowerStateCallback());
    }

    public void updatePowerState(final PowerType.Power.PowerState state) {
        builder.getPowerStateBuilder().setState(state);
        notifyChange();
    }

    public void setPowerState(final PowerType.Power.PowerState state) throws RSBBindingException {
        logger.debug("Setting [" + id + "] to PowerState [" + state.name() + "]");
        OpenhabCommand.Builder newBuilder = OpenhabCommand.newBuilder();
        newBuilder.setOnOff(PowerStateTransformer.transform(state)).setType(OpenhabCommand.CommandType.ONOFF);
        executeCommand(newBuilder);
    }

    public class SetPowerStateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                PowerPlugController.this.setPowerState(((PowerType.Power) request.getData()).getState());
                return new Event(String.class, "Ok");
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + PowerPlugController.this.getId() + "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }
}
