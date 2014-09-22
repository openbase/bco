/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.citec.dal.view.sensor;

import de.citec.dal.view.AbstractSensorPanel;
import rsb.Event;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.MotionSensorType;

/**
 *
 * @author nuc
 */
public class MotionSensor extends AbstractSensorPanel {

    @Override
    protected void addConverter() {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(MotionSensorType.MotionSensor.getDefaultInstance()));
    }

    @Override
    public void internalNotify(Event event) {
        MotionSensor motionSensor = (MotionSensor) event.getData();
//        switch (motionSensor.getState() ) {
//            
//        }
    }
    
}
