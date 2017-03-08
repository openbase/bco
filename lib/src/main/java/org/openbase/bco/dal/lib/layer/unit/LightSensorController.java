/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.dal.lib.layer.unit;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;
import rst.domotic.unit.dal.LightSensorDataType.LightSensorData;

/**
 *
 * @author pleminoq
 */
public class LightSensorController extends AbstractDALUnitController<LightSensorData, LightSensorData.Builder> implements LightSensor {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LightSensorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(IlluminanceState.getDefaultInstance()));
    }

    public LightSensorController(final UnitHost unitHost, LightSensorData.Builder builder) throws org.openbase.jul.exception.InstantiationException, CouldNotPerformException {
        super(LightSensorController.class, unitHost, builder);
    }

    public void updateIlluminanceStateProvider(final IlluminanceState illuminanceState) throws CouldNotPerformException {
        logger.debug("Apply illuminanceState Update[" + illuminanceState + "] for " + this + ".");

        try (ClosableDataBuilder<LightSensorData.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setIlluminanceState(illuminanceState);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply illuminanceState Update[" + illuminanceState + "] for " + this + "!", ex);
        }
    }

    @Override
    public IlluminanceState getIlluminanceState() throws NotAvailableException {
        try {
            return getData().getIlluminanceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("illuminanceState", ex);
        }
    }

}
