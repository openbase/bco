/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.OpenClosedType;
import rst.homeautomation.state.OpenClosedType.OpenClosed.OpenClosedState;
import rst.homeautomation.unit.ReedSwitchType;
import rst.homeautomation.unit.ReedSwitchType.ReedSwitch;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchController extends AbstractUnitController<ReedSwitch, ReedSwitch.Builder> implements ReedSwitchInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedSwitchType.ReedSwitch.getDefaultInstance()));
    }

    public ReedSwitchController(final String label, DeviceInterface device, ReedSwitch.Builder builder) throws InstantiationException {
        super(ReedSwitchController.class, label, device, builder);
    }

    public void updateReedSwitch(final OpenClosedType.OpenClosed.OpenClosedState state) {
        data.getReedSwitchStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public OpenClosedState getReedSwitch() throws CouldNotPerformException {
        logger.debug("Getting [" + label + "] State: [" + data.getReedSwitchState() + "]");
        return data.getReedSwitchState().getState();
    }
}
