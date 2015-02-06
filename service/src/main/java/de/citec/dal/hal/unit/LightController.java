package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.Event;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.LightType;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author thuxohl
 */
public class LightController extends AbstractUnitController<LightType.Light, LightType.Light.Builder> implements LightInterface{

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(LightType.Light.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    public LightController(String id, final String label, DeviceInterface hardwareUnit, LightType.Light.Builder builder) throws InstantiationException {
        super(id, label, hardwareUnit, builder);
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("setPowerState", new SetPowerStateCallback());
    }

    public void updatePowerState(final PowerType.Power.PowerState state) {
        data.getPowerStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public void setPowerState(final PowerType.Power.PowerState state) throws CouldNotPerformException {
        logger.debug("Setting [" + id + "] to PowerState [" + state.name() + "]");
        throw new UnsupportedOperationException("Not supported yet.");
//        OpenhabCommand.Builder newBuilder = OpenhabCommand.newBuilder();
//        newBuilder.setOnOff(PowerStateTransformer.transform(state)).setType(OpenhabCommand.CommandType.ONOFF);
//        executeCommand(newBuilder);
    }

    @Override
    public PowerType.Power.PowerState getPowerState() throws CouldNotPerformException {
        return data.getPowerState().getState();
    }

    public class SetPowerStateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                throw new UnsupportedOperationException("Not supported yet.");
//                LightController.this.setPowerState(((PowerType.Power) request.getData()).getState());
//                return RSBCommunicationService.RPC_FEEDBACK_OK;
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setPowerState] for [" + LightController.this.getId() + "]", ex);
                throw ex;
            }
        }
    }
}
