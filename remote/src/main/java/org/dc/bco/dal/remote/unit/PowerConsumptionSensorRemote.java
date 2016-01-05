/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import org.dc.bco.dal.lib.layer.unit.PowerConsumptionSensorInterface;
import org.dc.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.homeautomation.unit.PowerConsumptionSensorType.PowerConsumptionSensor;

/**
 *
 * @author thuxohl
 */
public class PowerConsumptionSensorRemote extends DALRemoteService<PowerConsumptionSensor> implements PowerConsumptionSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionState.getDefaultInstance()));
    }

    public PowerConsumptionSensorRemote() {
    }

    @Override
    public void notifyUpdated(PowerConsumptionSensor data) {
    }

    @Override
    public PowerConsumptionState getPowerConsumption() throws CouldNotPerformException {
        return getData().getPowerConsumptionState();
    }
}
