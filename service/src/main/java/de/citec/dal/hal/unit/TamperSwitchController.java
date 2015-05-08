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

    public void updateTamper(TamperType.Tamper value) throws CouldNotPerformException {
        logger.debug("Apply tamper Update[" + value + "] for " + this + ".");
        try (ClosableDataBuilder<TamperSwitch.Builder> dataBuilder = getDataBuilder(this)) {

            //TODO tamino: need to be tested! Please write an unit test.
            if (value.getState() == TamperType.Tamper.TamperState.TAMPER) {
                value = value.toBuilder().setLastDetection(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis()).build()).build();
            }

            dataBuilder.getInternalBuilder().setTamperState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply tamper Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public TamperType.Tamper getTamper() throws NotAvailableException {
        try {
            return getData().getTamperState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("tamper", ex);
        }
    }
}
