package org.openbase.bco.dal.remote.layer.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Factory;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.Collection;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface ServiceRemoteFactory extends Factory<AbstractServiceRemote, ServiceType> {

    /**
     * Creates and initializes a service remote out of the given service type
     * and a collection of unitConfigs.
     *
     * @param serviceType The remote service type.
     * @param unitConfigs The collection of units which are controlled by the new service remote instance.
     * @return the new created service remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     * @throws InterruptedException     is thrown if the current thread is externally interrupted.
     */
    AbstractServiceRemote<?, ?> newInitializedInstance(final ServiceType serviceType, final Collection<UnitConfig> unitConfigs) throws CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes a service remote out of the given service type, a collection of unitConfigs and a flag
     * determining if infrastructure units should be filtered.
     *
     * @param serviceType               The remote service type.
     * @param unitConfigs               The collection of units which are controlled by the new service remote instance.
     * @param filterInfrastructureUnits The flag determining if the created service remote filters infrastructure units.
     * @return the new created service remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     * @throws InterruptedException     is thrown if the current thread is externally interrupted.
     */
    AbstractServiceRemote<?, ?> newInitializedInstance(final ServiceType serviceType, final Collection<UnitConfig> unitConfigs, final boolean filterInfrastructureUnits) throws CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes a service remote out of the given service type
     * and unitConfig.
     *
     * @param serviceType The remote service type.
     * @param unitConfig  The unit which is controlled by the new service remote instance.
     * @return the new created service remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     * @throws InterruptedException     is thrown if the current thread is externally interrupted.
     */
    AbstractServiceRemote<?, ?> newInitializedInstance(final ServiceType serviceType, final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes a service remote out of the given service type
     * and a collection of unitConfigs.
     *
     * @param serviceType The remote service type.
     * @param unitIds     The collection of units which are controlled by the new service remote instance.
     * @return the new created service remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     * @throws InterruptedException     is thrown if the current thread is externally interrupted.
     */
    AbstractServiceRemote<?, ?> newInitializedInstanceByIds(final ServiceType serviceType, final Collection<String> unitIds) throws CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes a service remote out of the given service type
     * and unitID.
     *
     * @param serviceType The remote service type.
     * @param unitId      The unit id of the unit which should be controlled by the new service remote instance.
     * @return the new created service remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     * @throws InterruptedException     is thrown if the current thread is externally interrupted.
     */
    AbstractServiceRemote<?, ?> newInitializedInstanceById(final ServiceType serviceType, final String unitId) throws CouldNotPerformException, InterruptedException;

    /**
     * Creates and initializes a service remote out of the given service type
     * and unitConfig.
     *
     * @param serviceType The remote service type.
     * @param unitConfig  The unit which is controlled by the new service remote instance.
     * @return the new created service remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     * @throws InterruptedException     is thrown if the current thread is externally interrupted.
     */
    @Deprecated
    default AbstractServiceRemote<?, ?> createAndInitServiceRemote(final ServiceType serviceType, final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        return newInitializedInstance(serviceType, unitConfig);
    }

    /**
     * Creates a service remote out of the given service type.
     *
     * @param serviceType The remote service type.
     * @return the new created unit remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     */
    @Deprecated
    default AbstractServiceRemote<?, ?> createServiceRemote(final ServiceType serviceType) throws CouldNotPerformException {
        try {
            return newInstance(serviceType);
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Current thread was interrupted!", ex);
        }
    }

    /**
     * Creates and initializes a service remote out of the given service type
     * and a collection of unitConfigs.
     * <p>
     * newInitializedInstance
     *
     * @param serviceType The remote service type.
     * @param unitConfigs The collection of units which are controlled by the new service remote instance.
     * @return the new created service remote.
     * @throws CouldNotPerformException is thrown if any error occurs during the creation.
     */
    @Deprecated
    default AbstractServiceRemote<?, ?> createAndInitServiceRemote(final ServiceType serviceType, final Collection<UnitConfig> unitConfigs) throws CouldNotPerformException {
        try {
            return newInitializedInstance(serviceType, unitConfigs);
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("Current thread was interrupted!", ex);
        }
    }
}
