package org.openbase.bco.dal.remote.unit;

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

import java.util.HashMap;
import java.util.Map;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.rsb.com.AbstractIdentifiableRemote;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRemotePool {

    private Map<Class, Map<String, UnitRemote<?, UnitConfig>>> pool;
    private UnitRemoteFactory factory;
    private DeviceRegistryRemote deviceRegistryRemote;

    public UnitRemotePool() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        this(UnitRemoteFactoryImpl.getInstance());
    }

    public UnitRemotePool(UnitRemoteFactory factory) throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.pool = new HashMap<>();
            this.factory = factory;
            this.deviceRegistryRemote = new DeviceRegistryRemote();
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            deviceRegistryRemote.init();
            deviceRegistryRemote.activate();
            initAllUnitRemotes();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private void initAllUnitRemotes() throws CouldNotPerformException, InterruptedException {
        for (UnitConfig unitConfig : deviceRegistryRemote.getUnitConfigs()) {
            UnitRemote<?, UnitConfig> unitRemote = factory.newInitializedInstance(unitConfig);

            if (!pool.containsKey(unitRemote.getClass())) {
                pool.put(unitRemote.getClass(), new HashMap<>());
            }

            pool.get(unitRemote.getClass()).put(unitRemote.getId(), unitRemote);

        }
    }

    public void activate() throws InterruptedException, CouldNotPerformException {
        for (Map<String, UnitRemote<?, UnitConfig>> unitCollection : pool.values()) {
            for (UnitRemote remote : unitCollection.values()) {
                remote.activate();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <R extends AbstractIdentifiableRemote> R getUnitRemote(final String unitId, final Class<? extends R> remoteClass) {
        return (R) pool.get(remoteClass).get(unitId);
    }
}
