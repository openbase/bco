/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.agent.remote;

import org.dc.bco.dal.remote.unit.AbstractConfigurableRemote;
import org.dc.bco.manager.agent.lib.Agent;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.com.RPCHelper;
import org.dc.jul.extension.rsb.scope.ScopeProvider;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.agent.AgentDataType.AgentData;
import rst.homeautomation.state.ActivationStateType;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class AgentRemote extends AbstractConfigurableRemote<AgentData, AgentConfig> implements Agent {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    @Override
    public void notifyUpdated(AgentData data) throws CouldNotPerformException {
        
    }

    @Override
    public void setActivationState(ActivationStateType.ActivationState activation) throws CouldNotPerformException {
        RPCHelper.callRemoteMethod(activation, this);
    }

    @Override
    public ScopeProvider getScopeProvider(final AgentConfig config) {
        return () -> ScopeTransformer.transform(config.getScope());
    }
}
