package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.LightType;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author thuxohl
 */
public class LightController extends AbstractUnitController<LightType.Light, LightType.Light.Builder> implements LightInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LightType.Light.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    private final PowerService powerService;

    public LightController(final String label, DeviceInterface device, LightType.Light.Builder builder) throws InstantiationException {
        this(label, device, builder, device.getDefaultServiceFactory());
    }

    public LightController(final String label, DeviceInterface device, LightType.Light.Builder builder, ServiceFactory serviceFactory) throws InstantiationException {
        super(LightController.class, label, device, builder);
        this.powerService = serviceFactory.newPowerService(device, this);
    }

    public void updatePower(final PowerType.Power.PowerState state) {
        data.getPowerStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public void setPower(final PowerType.Power.PowerState state) throws CouldNotPerformException {
        logger.debug("Setting [" + label + "] to Power [" + state.name() + "]");
        powerService.setPower(state);
    }

    @Override
    public PowerType.Power.PowerState getPower() throws CouldNotPerformException {
        return data.getPowerState().getState();
    }
}
