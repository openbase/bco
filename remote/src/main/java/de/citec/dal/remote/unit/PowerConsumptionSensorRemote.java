/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.PowerConsumptionSensorInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.PowerConsumptionSensorType;

/**
 *
 * @author thuxohl
 */
public class PowerConsumptionSensorRemote extends DALRemoteService<PowerConsumptionSensorType.PowerConsumptionSensor> implements PowerConsumptionSensorInterface {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerConsumptionSensorType.PowerConsumptionSensor.getDefaultInstance()));
    }

    public PowerConsumptionSensorRemote() {
    }

    @Override
    public void notifyUpdated(PowerConsumptionSensorType.PowerConsumptionSensor data) {
    }   

    @Override
    public Double getPowerConsumption() throws CouldNotPerformException {
        return this.getData().getConsumption();
    }
}
