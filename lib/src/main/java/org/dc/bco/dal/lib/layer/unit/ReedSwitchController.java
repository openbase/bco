/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;


import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
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

    public ReedSwitchController(final UnitHost unitHost, ReedSwitch.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(ReedSwitchController.class, unitHost, builder);
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
