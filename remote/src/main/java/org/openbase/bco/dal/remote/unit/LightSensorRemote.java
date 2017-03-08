/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.dal.remote.unit;

import org.openbase.bco.dal.lib.layer.unit.LightSensor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;
import rst.domotic.unit.dal.LightSensorDataType.LightSensorData;

/**
 *
 * @author pleminoq
 */
public class LightSensorRemote extends AbstractUnitRemote<LightSensorData> implements LightSensor {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LightSensorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(IlluminanceState.getDefaultInstance()));
    }

    public LightSensorRemote() {
        super(LightSensorData.class);
    }

    @Override
    public void notifyDataUpdate(LightSensorData data) {
    }

    @Override
    public IlluminanceState getIlluminanceState() throws NotAvailableException {
        try {
            return getData().getIlluminanceState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("BrightnessState", ex);
        }
    }

}
