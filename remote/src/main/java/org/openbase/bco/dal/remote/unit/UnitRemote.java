package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.pattern.ConfigurableRemote;
import rsb.Scope;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.rsb.ScopeType;

/**
 *  TODO Release: remove unused parameter CONFIG
 * 
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M> Message
 * @param <CONFIG> Configuration
 */
public interface UnitRemote<M, CONFIG> extends Unit, Service, ConfigurableRemote<String, M, UnitConfig> {

    /**
     * Method initializes this unit remote instance via it's remote controller scope.
     * @param scope the scope which is used to reach the remote controller.
     * @throws InitializationException is thrown in case the remote could not be initialized with the given scope.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    void init(ScopeType.Scope scope) throws InitializationException, InterruptedException;

    /**
     * Method initializes this unit remote instance via it's remote controller scope.
     * @param scope the scope which is used to reach the remote controller.
     * @throws InitializationException is thrown in case the remote could not be initialized with the given scope.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    void init(Scope scope) throws InitializationException, InterruptedException;

    /**
     * Method initializes this unit remote instance via the given id.
     * @param id the unit id which is used to resolve the remote controller scope.
     * @throws InitializationException is thrown in case the remote could not be initialized with the given id.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    void initById(final String id) throws InitializationException, InterruptedException;

    /**
     * Method initializes this unit remote instance via the given label.
     * @param label the unit label which is used to resolve the remote controller scope.
     * @throws InitializationException is thrown in case the remote could not be initialized with the given label.
     * @throws InterruptedException is thrown in case the thread is externally interrupted.
     */
    void initByLabel(final String label) throws InitializationException, InterruptedException;
}
