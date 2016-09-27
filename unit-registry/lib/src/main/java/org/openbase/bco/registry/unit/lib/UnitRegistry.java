package org.openbase.bco.registry.unit.lib;

/*
 * #%L
 * REM UnitRegistry Library
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
import java.util.List;
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author mpohling
 */
public interface UnitRegistry extends Shutdownable {

    public Future<UnitConfig> registerUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException;

    public Future<UnitConfig> updateUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException;

    public Future<UnitConfig> removeUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException;

    public Boolean containsUnitConfig(UnitConfig unitConfig) throws CouldNotPerformException;

    public Boolean containsUnitConfigById(String unitConfigId) throws CouldNotPerformException;

    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;

    public Boolean isUnitConfigRegistryReadOnly() throws CouldNotPerformException;

    /**
     * Method returns true if the underling registry is marked as consistent.
     *
     * @return if the UnitConfig registry is consistent
     * @throws CouldNotPerformException if the check fails
     */
    public Boolean isUnitConfigRegistryConsistent() throws CouldNotPerformException;

    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    public Boolean containsUnitTemplate(final UnitTemplate unitTemplate) throws CouldNotPerformException;

    public Boolean containsUnitTemplateById(final String unitTemplateId) throws CouldNotPerformException;

    public UnitTemplate getUnitTemplateById(final String unitTemplate) throws CouldNotPerformException, InterruptedException;

    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException;

    public UnitTemplate getUnitTemplateByType(final UnitType type) throws CouldNotPerformException;

    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException;

    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException;

}
