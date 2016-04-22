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
import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.pattern.ConfigurableRemote;
import rsb.Scope;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 * @param <M> Message
 * @param <CONFIG> Configuration
 */
public interface UnitRemote<M, CONFIG> extends Unit, Service, ConfigurableRemote<String, M, UnitConfig> {

    void init(ScopeType.Scope scope) throws InitializationException, InterruptedException;

    void init(Scope scope) throws InitializationException, InterruptedException;

    void init(String scope) throws InitializationException, InterruptedException;

    void initById(final String id) throws InitializationException, InterruptedException;

    void initByLabel(final String label) throws InitializationException, InterruptedException;

}
