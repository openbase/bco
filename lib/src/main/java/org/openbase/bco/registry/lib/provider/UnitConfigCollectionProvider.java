package org.openbase.bco.registry.lib.provider;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;

public interface UnitConfigCollectionProvider {

    public List<UnitConfig> getUnitConfigs() throws CouldNotPerformException;

    @RPCMethod
    public Boolean containsUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    @RPCMethod
    public UnitConfig getUnitConfigById(final String unitConfigId) throws CouldNotPerformException;

    /**
     * Method returns a list of all globally registered units of the given {@code type}.
     * <p>
     * Note: The type {@code UnitType.UNKNOWN} is used as wildcard and will return a list of all registered units.
     *
     * @param type the unit type to filter.
     * @return a list of unit configurations.
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    public List<UnitConfig> getUnitConfigs(final UnitType type) throws CouldNotPerformException;
}
