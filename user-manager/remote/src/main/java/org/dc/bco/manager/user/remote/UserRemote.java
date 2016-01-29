/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.user.remote;

import org.dc.bco.dal.remote.unit.AbstractConfigurableRemote;
import org.dc.bco.manager.user.lib.User;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.scope.ScopeProvider;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.converter.UserData;
import rst.authorization.UserConfigType.UserConfig;
import rst.homeautomation.state.ActivationStateType.ActivationState;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UserRemote extends AbstractConfigurableRemote<UserData, UserConfig> implements User {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    @Override
    public void notifyUpdated(UserData data) throws CouldNotPerformException {ca
    }

    @Override
    public ScopeProvider getScopeProvider(final UserConfig config) {
        return () -> ScopeTransformer.transform(config.getScope());
    }
}
