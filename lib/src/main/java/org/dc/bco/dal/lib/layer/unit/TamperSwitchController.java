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
import rst.homeautomation.state.TamperStateType.TamperState;
import rst.homeautomation.unit.TamperSwitchType.TamperSwitch;
import rst.homeautomation.unit.UnitConfigType;
import rst.timing.TimestampType;

/**
 *
 * @author thuxohl
 */
public class TamperSwitchController extends AbstractUnitController<TamperSwitch, TamperSwitch.Builder> implements TamperSwitchInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperSwitch.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperState.getDefaultInstance()));
    }

    public TamperSwitchController(final UnitHost unitHost, final TamperSwitch.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(TamperSwitchController.class, unitHost, builder);
    }

    public void updateTamper(final TamperState state) throws CouldNotPerformException {
        
        logger.debug("Apply tamper Update[" + state + "] for " + this + ".");
        
        try (ClosableDataBuilder<TamperSwitch.Builder> dataBuilder = getDataBuilder(this)) {
            
            TamperState.Builder tamperStateBuilder = dataBuilder.getInternalBuilder().getTamperStateBuilder();
            
            // Update value
            tamperStateBuilder.setValue(state.getValue());
            
            // Update timestemp if necessary
            if (state.getValue()== TamperState.State.TAMPER) {
                //TODO tamino: need to be tested! Please write an unit test.
                tamperStateBuilder.setLastDetection(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis()));
            }

            dataBuilder.getInternalBuilder().setTamperState(tamperStateBuilder);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply tamper Update[" + state + "] for " + this + "!", ex);
        }
    }

    @Override
    public TamperState getTamper() throws NotAvailableException {
        try {
            return getData().getTamperState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("tamper", ex);
        }
    }
}
