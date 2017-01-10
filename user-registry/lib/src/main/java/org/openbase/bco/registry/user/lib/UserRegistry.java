package org.openbase.bco.registry.user.lib;

/*
 * #%L
 * BCO Registry User Library
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
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UserRegistry extends Shutdownable {

    public Future<UnitConfig> registerUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    public Boolean containsUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    public Boolean containsUserConfigById(String userConfigId) throws CouldNotPerformException;

    public Future<UnitConfig> updateUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    public Future<UnitConfig> removeUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    public UnitConfig getUserConfigById(final String userConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getUserConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getUserConfigsByAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    public Boolean isUserConfigRegistryReadOnly() throws CouldNotPerformException;

    public Future<UnitConfig> registerAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    public Boolean containsAuthorizationGroupConfigById(String groupConfigId) throws CouldNotPerformException;

    public Future<UnitConfig> updateAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    public Future<UnitConfig> removeAuthorizationGroupConfig(UnitConfig groupConfig) throws CouldNotPerformException;

    public UnitConfig getAuthorizationGroupConfigById(final String groupConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getAuthorizationGroupConfigs() throws CouldNotPerformException;

    public List<UnitConfig> getAuthorizationGroupConfigsbyUserConfig(UnitConfig userConfig) throws CouldNotPerformException;

    public Boolean isAuthorizationGroupConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the user config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    public Boolean isUserConfigRegistryConsistent() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the authorization group config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    public Boolean isAuthorizationGroupConfigRegistryConsistent() throws CouldNotPerformException;
}
