/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import com.google.protobuf.GeneratedMessage;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.scope.ScopeProvider;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 * @param <M>
 */
public abstract class AbstractUnitRemote<M extends GeneratedMessage> extends AbstractConfigurableRemote<M, UnitConfig> {

    @Override
    public ScopeProvider getScopeProvider(final UnitConfig config) {
        return () -> {
            try {
                if (config == null) {
                    throw new NotAvailableException("config");
                }

                if (!config.hasScope()) {
                    throw new NotAvailableException("config.scope");
                }
                return ScopeTransformer.transform(config.getScope());
            } catch (NotAvailableException ex) {
                throw new NotAvailableException(ScopeProvider.class, config.getLabel(), ex);
            }
        };
    }
}
