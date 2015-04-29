/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.TamperType;
import rst.homeautomation.unit.TamperSwitchType;
import rst.homeautomation.unit.TamperSwitchType.TamperSwitch;
import rst.homeautomation.unit.UnitConfigType;
import rst.timing.TimestampType;

/**
 *
 * @author thuxohl
 */
public class TamperSwitchController extends AbstractUnitController<TamperSwitch, TamperSwitch.Builder> implements TamperSwitchInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperSwitchType.TamperSwitch.getDefaultInstance()));
    }

    public TamperSwitchController(final UnitConfigType.UnitConfig config, Device device, TamperSwitch.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, TamperSwitchController.class, device, builder);
    }

    public void updateTamper(final TamperType.Tamper.TamperState state) {
        logger.debug("Updating tamper of [" + this + "] to [" + state + "]");
        data.getTamperStateBuilder().setState(state);
        if (state == TamperType.Tamper.TamperState.TAMPER) {
            data.getTamperStateBuilder().setLastDetection(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis()).build());
        }
        notifyChange();
    }

    @Override
    public TamperType.Tamper getTamper() throws CouldNotPerformException {
        return data.getTamperState();
    }
}
