/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.data.transform.HSVColorTransformer;
import de.citec.dal.data.transform.PowerStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHALController;
import rsb.Event;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.AmbientLightType;
import rst.homeautomation.states.PowerType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author nuc
 */
public class AmbientLightController extends AbstractHALController<AmbientLightType.AmbientLight, AmbientLightType.AmbientLight.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(AmbientLightType.AmbientLight.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HSVColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    public AmbientLightController(String id, HardwareUnit hardwareUnit, AmbientLightType.AmbientLight.Builder builder) throws RSBBindingException {
        super(id, hardwareUnit, builder);
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("setColor", new SetColorCallback());
        server.addMethod("setPowerState", new SetPowerStateCallback());

    }

    public void updatePowerState(final PowerType.Power.PowerState state) {
        builder.getStateBuilder().setState(state);
        notifyChange();
    }

    public void setPowerState(final PowerType.Power.PowerState state) throws RSBBindingException {
        logger.debug("Setting [" + id + "] to PowerState [" + state.name() + "]");
        sendCommand(PowerStateTransformer.transform(state));
    }

    public class SetPowerStateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                AmbientLightController.this.setPowerState(((PowerType.Power) request.getData()).getState());
                return new Event(String.class, "Ok");
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + AmbientLightController.this.getId() + "]", ex);
                return new Event(String.class, "Failed");
            }
        }
    }

    public void updateColor(final HSVColor color) {
        builder.setColor(color);
        notifyChange();
    }

    public void setColor(final HSVColor color) throws RSBBindingException {
        logger.debug("Setting [" + id + "] to HSVColor[" + color.getHue() + "|" + color.getSaturation() + "|" + color.getValue() + "]");
        sendCommand(HSVColorTransformer.transform(color));
    }

    public class SetColorCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                AmbientLightController.this.setColor(((HSVColor) request.getData()));
                return new Event(String.class, "Ok");
            } catch (Exception ex) {
                logger.warn("Could not invoke method for " + AmbientLightController.this, ex);
                return new Event(String.class, "Failed");
            }
        }
    }
}
