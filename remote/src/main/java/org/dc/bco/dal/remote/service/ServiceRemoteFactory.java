/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

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

import java.util.Collection;
import org.dc.bco.dal.remote.unit.DALRemoteService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface ServiceRemoteFactory {

    /**
     * Creates and initializes a service remote out of the given service type
     * and a collection of unitConfigs.
     *
     * @param serviceType
     * @param unitConfigs
     * @return the new created service remote.
     * @throws CouldNotPerformException
     */
    public AbstractServiceRemote createAndInitServiceRemote(final ServiceType serviceType, final Collection<UnitConfig> unitConfigs) throws CouldNotPerformException;

    /**
     * Creates and initializes a service remote out of the given service type
     * and unitConfig.
     *
     * @param serviceType
     * @param unitConfig
     * @return the new created service remote.
     * @throws CouldNotPerformException
     */
    public AbstractServiceRemote createAndInitServiceRemote(final ServiceType serviceType, final UnitConfig unitConfig) throws CouldNotPerformException;

    /**
     * Creates a service remote out of the given service type.
     *
     * @param serviceType
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     */
    public AbstractServiceRemote createServiceRemote(final ServiceType serviceType) throws CouldNotPerformException;
}
