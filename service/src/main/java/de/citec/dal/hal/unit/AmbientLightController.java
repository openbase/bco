/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.service.BrightnessService;
import de.citec.dal.hal.service.ColorService;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.rsb.RSBCommunicationService;
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
public class AmbientLightController extends AbstractUnitController<AmbientLightType.AmbientLight, AmbientLightType.AmbientLight.Builder> implements AmbientLightInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AmbientLightType.AmbientLight.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSVColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    private final ColorService colorService;
    private final BrightnessService brightnessService;
    private final PowerService powerService;

    public AmbientLightController(final String label, final DeviceInterface device, final AmbientLightType.AmbientLight.Builder builder) throws InstantiationException {
        this(label, device, builder, device.getDefaultServiceFactory());
    }
    
    public AmbientLightController(final String label, final DeviceInterface device, final AmbientLightType.AmbientLight.Builder builder, final ServiceFactory serviceFactory) throws InstantiationException {
        super(AmbientLightController.class , label, device, builder);
        this.powerService = serviceFactory.newPowerService(device, this);
        this.colorService = serviceFactory.newColorService(device, this);
        this.brightnessService = serviceFactory.newBrightnessService(device, this);
    }

    
    //TODO: mpohling: generate methods registering and implement one callback per service not within unit.
    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("setColor", new SetColorCallback());
        server.addMethod("setPowerState", new SetPowerStateCallback());
        server.addMethod("setBrightness", new SetBrightnessCallback());
    }

    public void updatePower(final PowerType.Power.PowerState state) {
        logger.debug("Update "+name+"[" + label + "] to PowerState [" + state.name() + "]");
        data.getPowerStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public void setPower(final PowerType.Power.PowerState state) throws CouldNotPerformException {
        logger.debug("Set "+name+"[" + label + "] to PowerState [" + state.name() + "]");
        powerService.setPower(state);
    }

    @Override
    public PowerType.Power.PowerState getPowerState() throws CouldNotPerformException {
        return data.getPowerState().getState();
    }

    public class SetPowerStateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                AmbientLightController.this.setPower(((PowerType.Power) request.getData()).getState());
                return RSBCommunicationService.RPC_FEEDBACK_OK;
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setPowerState] for [" + AmbientLightController.this.getName()+ "]", ex);
                throw ex;
            }
        }
    }

    public void updateColor(final HSVColor color) {
        data.setColor(color);
        notifyChange();
    }

    @Override
    public HSVColor getColor() {
        return data.getColor();
    }

    @Override
    public void setColor(final HSVColor color) throws CouldNotPerformException {
        logger.debug("Set "+name+"[" + label + "] to HSVColor[" + color.getHue() + "|" + color.getSaturation() + "|" + color.getValue() + "]");
        colorService.setColor(color);
    }

    public class SetColorCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                AmbientLightController.this.setColor(((HSVColor) request.getData()));
                return RSBCommunicationService.RPC_FEEDBACK_OK;
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setColor] for " + AmbientLightController.this, ex);
                throw ex;
            }
        }
    }
	
    public void updateBrightness(Double value) {
        updateColor(data.getColor().newBuilderForType().setValue(value).build());
    }

    @Override
    public Double getBrightness() {
        return data.getColor().getValue();
    }

    @Override
    public void setBrightness(Double brightness) throws CouldNotPerformException {
        logger.debug("Set "+name+"[" + label + "] to Brightness[" + brightness + "]");
        brightnessService.setBrightness(brightness);
    }

    public class SetBrightnessCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                AmbientLightController.this.setBrightness(((double) request.getData()));
                return RSBCommunicationService.RPC_FEEDBACK_OK;
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setBrightness] for " + AmbientLightController.this, ex);
                throw ex;
            }
        }
    }
}
