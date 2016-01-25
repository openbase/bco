/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import com.google.protobuf.GeneratedMessage;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.scope.ScopeProvider;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 * @param <M>
 * @param <CONFIG>
 */
public abstract class AbstractConfigurableRemote<M extends GeneratedMessage, CONFIG> extends DALRemoteService<M> {

    protected CONFIG config;

    public void init(final CONFIG config) throws InitializationException {
        try {
            super.init(getScopeProvider(config).getScope());
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public abstract ScopeProvider getScopeProvider(final CONFIG config);

    public CONFIG getConfig() throws NotAvailableException {
        if (config == null) {
            throw new NotAvailableException("config");
        }
        return config;
    }
}
