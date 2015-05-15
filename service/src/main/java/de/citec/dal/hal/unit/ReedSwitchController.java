/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ReedSwitchStateType.ReedSwitchState;
import rst.homeautomation.unit.ReedSwitchType.ReedSwitch;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchController extends AbstractUnitController<ReedSwitch, ReedSwitch.Builder> implements ReedSwitchInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedSwitch.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedSwitchState.getDefaultInstance()));
    }

    public ReedSwitchController(final UnitConfigType.UnitConfig config, Device device, ReedSwitch.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, ReedSwitchController.class, device, builder);
    }

    public void updateReedSwitch(final ReedSwitchState.State value) throws CouldNotPerformException {
        logger.debug("Apply reed switch Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<ReedSwitch.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().getReedSwitchStateBuilder().setValue(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply reed switch Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public ReedSwitchState getReedSwitch() throws NotAvailableException {
        try {
            return getData().getReedSwitchState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("reed switch", ex);
        }
    }
}
