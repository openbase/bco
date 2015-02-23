/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.HandleSensorInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.OpenClosedTiltedType;
import rst.homeautomation.unit.HandleSensorType;

/**
 *
 * @author thuxohl
 */
public class HandleSensorRemote extends DALRemoteService<HandleSensorType.HandleSensor> implements HandleSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleSensorType.HandleSensor.getDefaultInstance()));
    }

    public HandleSensorRemote() {
    }

    @Override
    public void notifyUpdated(HandleSensorType.HandleSensor data) {
    }

    @Override
    public OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState getHandle() throws CouldNotPerformException {
        return this.getData().getHandleState().getState();
    }

}
