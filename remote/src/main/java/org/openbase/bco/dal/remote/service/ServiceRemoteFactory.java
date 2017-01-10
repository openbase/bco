package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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

import java.util.Collection;
import org.openbase.jul.exception.CouldNotPerformException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface ServiceRemoteFactory {

    /**
     * Creates and initializes a service remote out of the given service type
     * and a collection of unitConfigs.
     *
     * @param serviceType The remote service type.
     * @param unitConfigs The collection of units which are controlled by the new service remote instance.
     * @return the new created service remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     * @throws InterruptedException is thrown if the current thread is externally interrupted.
     */
    public AbstractServiceRemote createAndInitServiceRemote(final ServiceType serviceType, final Collection<UnitConfig> unitConfigs) throws CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes a service remote out of the given service type
     * and unitConfig.
     *
     * @param serviceType The remote service type.
     * @param unitConfig The unit which is controlled by the new service remote instance.
     * @return the new created service remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     * @throws InterruptedException is thrown if the current thread is externally interrupted.
     */
    public AbstractServiceRemote createAndInitServiceRemote(final ServiceType serviceType, final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException;

    /**
     * Creates a service remote out of the given service type.
     *
     * @param serviceType The remote service type.
     * @return the new created unit remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     */
    public AbstractServiceRemote createServiceRemote(final ServiceType serviceType) throws CouldNotPerformException;
}
