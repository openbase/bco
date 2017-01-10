package org.openbase.bco.registry.scene.lib;

/*
 * #%L
 * BCO Registry Scene Library
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
public interface SceneRegistry extends Shutdownable {

    public Future<UnitConfig> registerSceneConfig(UnitConfig sceneUnitConfig) throws CouldNotPerformException;

    public Boolean containsSceneConfig(UnitConfig sceneUnitConfig) throws CouldNotPerformException;

    public Boolean containsSceneConfigById(String sceneUnitConfigId) throws CouldNotPerformException;

    public Future<UnitConfig> updateSceneConfig(UnitConfig sceneUnitConfig) throws CouldNotPerformException;

    public Future<UnitConfig> removeSceneConfig(UnitConfig sceneUnitConfig) throws CouldNotPerformException;

    public UnitConfig getSceneConfigById(final String sceneUnitConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getSceneConfigs() throws CouldNotPerformException;

    public Boolean isSceneConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the scene config registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    public Boolean isSceneConfigRegistryConsistent() throws CouldNotPerformException;
}
