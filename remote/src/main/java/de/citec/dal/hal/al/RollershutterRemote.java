/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.service.rsb.RSBRemoteService;
import de.citec.dal.util.DALException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.RollershutterType;
import rst.homeautomation.states.StopMoveType;
import rst.homeautomation.states.UpDownType;

/**
 *
 * @author thuxohl
 */
public class RollershutterRemote extends RSBRemoteService<RollershutterType.Rollershutter>{
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(RollershutterType.Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StopMoveType.StopMove.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UpDownType.UpDown.getDefaultInstance()));
    }

    public RollershutterRemote() {
    }

    public void setStopMoveState(final StopMoveType.StopMove.StopMoveState state) throws DALException {
        callMethodAsync("setStopMoveState", state);
    }
    
    public void setPosition(final float position) throws DALException {
        callMethodAsync("setPosition", position);
    }
    
    public void setUpDownState(final UpDownType.UpDown.UpDownState state) throws DALException {
        callMethodAsync("setUpDownState", state);
    }
    
    @Override
    public void notifyUpdated(RollershutterType.Rollershutter data) {
    }
    
}
