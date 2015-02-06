/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.hal.unit.HandleSensorInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.HandleSensorType;
import rst.homeautomation.states.OpenClosedTiltedType;

/**
 *
 * @author thuxohl
 */
public class HandleSensorRemote extends RSBRemoteService<HandleSensorType.HandleSensor> implements HandleSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HandleSensorType.HandleSensor.getDefaultInstance()));
    }

    public HandleSensorRemote() {
    }

    @Override
    public void notifyUpdated(HandleSensorType.HandleSensor data) {
    }

    @Override
    public OpenClosedTiltedType.OpenClosedTilted.OpenClosedTiltedState getRotaryHandleState() throws CouldNotPerformException {
        return this.getData().getHandleState().getState();
    }

}
