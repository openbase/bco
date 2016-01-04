/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import org.dc.bco.coma.dem.lib.Device;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.HandleStateType.HandleState;
import rst.homeautomation.unit.HandleSensorType.HandleSensor;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class HandleSensorController extends AbstractUnitController<HandleSensor, HandleSensor.Builder> implements HandleSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleState.getDefaultInstance()));
    }

    public HandleSensorController(final UnitConfigType.UnitConfig config, Device device, HandleSensor.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, HandleSensorController.class, device, builder);
    }

    public void updateHandle(final HandleState.State value) throws CouldNotPerformException {
        logger.debug("Apply handle state Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<HandleSensor.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().getHandleStateBuilder().setValue(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply handle state Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public HandleState getHandle() throws NotAvailableException {
        try {
            return getData().getHandleState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("handle state", ex);
        }
    }
}
