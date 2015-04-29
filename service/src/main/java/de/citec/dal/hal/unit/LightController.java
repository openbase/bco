package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.PowerService;
import de.citec.dal.hal.service.ServiceFactory;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerType;
import rst.homeautomation.unit.LightType;
import rst.homeautomation.unit.UnitConfigType;

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

    public LightController(final UnitConfigType.UnitConfig config, Device device, LightType.Light.Builder builder) throws InstantiationException, CouldNotPerformException {
        this(config, device, builder, device.getDefaultServiceFactory());
    }

    public LightController(final UnitConfigType.UnitConfig config, Device device, LightType.Light.Builder builder, ServiceFactory serviceFactory) throws InstantiationException, CouldNotPerformException {
        super(config, LightController.class, device, builder);
        this.powerService = serviceFactory.newPowerService(device, this);
    }

    public void updatePower(final PowerType.Power.PowerState state) {
        logger.debug("Updating [" + getLabel() + "] to Power [" + state.name() + "]");
        data.getPowerStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public void setPower(final PowerType.Power.PowerState state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to Power [" + state.name() + "]");
        powerService.setPower(state);
    }

    @Override
    public PowerType.Power.PowerState getPower() throws CouldNotPerformException {
        return data.getPowerState().getState();
    }
}
