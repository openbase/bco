/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
