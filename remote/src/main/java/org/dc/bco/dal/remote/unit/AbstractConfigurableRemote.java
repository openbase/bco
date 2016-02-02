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
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.scope.ScopeProvider;
import org.dc.jul.iface.Configurable;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 * @param <M>
 * @param <CONFIG>
 */
public abstract class AbstractConfigurableRemote<M extends GeneratedMessage, CONFIG> extends DALRemoteService<M> implements Configurable<String, CONFIG> {

    protected CONFIG config;

    public void init(final CONFIG config) throws InitializationException {
        try {
            this.config = config;
            super.init(getScopeProvider(config).getScope());
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public abstract ScopeProvider getScopeProvider(final CONFIG config);

    @Override
    public CONFIG getConfig() throws NotAvailableException {
        if (config == null) {
            throw new NotAvailableException("config");
        }
        return config;
    }

    @Override
    public CONFIG updateConfig(CONFIG config) throws CouldNotPerformException {
        this.config = config;
        return config;
    }
}
