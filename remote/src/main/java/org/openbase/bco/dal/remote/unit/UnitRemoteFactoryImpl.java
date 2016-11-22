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
import java.util.concurrent.TimeUnit;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.EnumNotSupportedException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;
import rst.rsb.ScopeType.Scope;

/**
 * A unit remote factory which can be used to create unit remote instances out
 * of the unit registry delivered unit configurations.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UnitRemoteFactoryImpl implements UnitRemoteFactory {

    private static UnitRemoteFactory instance;

    private UnitRemoteFactoryImpl() {
    }

    /**
     * Method returns a new singelton instance of the unit factory.
     *
     * @return
     */
    public synchronized static UnitRemoteFactory getInstance() {
        if (instance == null) {
            instance = new UnitRemoteFactoryImpl();
        }
        return instance;
    }

    /**
     * Creates an unit remote out of the given unit configuration.
     *
     * @param config the unit configuration which defines the remote type.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     * @deprecated use newInstance instead!
     */
    @Deprecated
    @Override
    public AbstractUnitRemote createUnitRemote(final UnitConfig config) throws CouldNotPerformException {
        return (AbstractUnitRemote) newInstance(config);
    }

    /**
     * Creates and initializes an unit remote out of the given unit
     * configuration.
     *
     * @param config the unit configuration which defines the remote type and is
     * used for the remote initialization.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     * @throws org.openbase.jul.exception.InitializationException
     * * @deprecated use newInitializedInstance instead!
     */
    @Override
    @Deprecated
    public AbstractUnitRemote createAndInitUnitRemote(final UnitConfig config) throws CouldNotPerformException {
        try {
            return (AbstractUnitRemote) newInitializedInstance(config);
        } catch (InterruptedException ex) {
            throw new CouldNotPerformException("FATAL ERROR: Interrupted Exception was catched!!! Deprecated API in use!", ex);
        }
    }

    /**
     * Method resolves the unit remote class of the given unit config.
     *
     * @param config the unit config to detect the unit class.
     * @return the unit remote class is returned.
     * @throws CouldNotPerformException is thrown if something went wrong during class loading.
     */
    public static Class<? extends AbstractUnitRemote> loadUnitRemoteClass(final UnitConfig config) throws CouldNotPerformException {
        return loadUnitRemoteClass(config.getType());
    }

    /**
     * Method resolves the unit remote class of the given unit type.
     *
     * @param unitType the unit type to detect the unit class.
     * @return the unit remote class is returned.
     * @throws CouldNotPerformException is thrown if something went wrong during class loading.
     */
    public static Class<? extends AbstractUnitRemote> loadUnitRemoteClass(final UnitType unitType) throws CouldNotPerformException {
        try {
            String remoteClassName = null;
            // check unit type and load related class.
            if (UnitConfigProcessor.isBaseUnit(unitType)) {
                remoteClassName = "org.openbase.bco.manager." + unitType.name().toLowerCase() + "." + StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + "Remote";
            } else if (UnitConfigProcessor.isDalUnit(unitType)) {
                remoteClassName = AbstractUnitRemote.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + "Remote";
            } else {
                throw new EnumNotSupportedException(unitType, UnitRemoteFactoryImpl.class);
            }
            return (Class<? extends AbstractUnitRemote>) UnitRemoteFactoryImpl.class.getClassLoader().loadClass(remoteClassName);
        } catch (CouldNotPerformException | ClassNotFoundException ex) {
            throw new CouldNotPerformException("Could not detect unit remote class for UnitType[" + unitType.name() + "]!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param config {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     */
    @Override
    public UnitRemote newInstance(final UnitConfig config) throws InstantiationException {
        try {
            return newInstance(loadUnitRemoteClass(config));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException("Could not create unit remote!", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param unitRemoteClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     */
    @Override
    public <R extends AbstractUnitRemote> R newInstance(final Class<R> unitRemoteClass) throws InstantiationException {
        try {
            return unitRemoteClass.newInstance();
        } catch (java.lang.InstantiationException | IllegalAccessException ex) {
            throw new org.openbase.jul.exception.InstantiationException("Could not instantiate unit remote out of Class[" + unitRemoteClass.getName() + "]", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     */
    @Override
    public UnitRemote newInstance(final UnitType type) throws InstantiationException {
        try {
            return newInstance(loadUnitRemoteClass(type));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException("Could not create unit remote!", ex);
        }

    }

    /**
     * {@inheritDoc}
     *
     * @param unitId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     */
    @Override
    public UnitRemote newInstance(String unitId, long timeout, TimeUnit timeUnit) throws InstantiationException, CouldNotPerformException, InterruptedException {
        CachedUnitRegistryRemote.waitForData(timeout, timeUnit);
        return newInstance(CachedUnitRegistryRemote.getRegistry().getUnitConfigById(unitId));
    }

    /**
     * {@inheritDoc}
     *
     * @param scope {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     */
    @Override
    public UnitRemote newInstance(ScopeType.Scope scope, long timeout, TimeUnit timeUnit) throws InstantiationException, CouldNotPerformException, InterruptedException {
        CachedUnitRegistryRemote.waitForData(timeout, timeUnit);
        return newInstance(CachedUnitRegistryRemote.getRegistry().getUnitConfigByScope(scope));
    }

    /**
     * {@inheritDoc}
     *
     * @param config {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public UnitRemote newInitializedInstance(final UnitConfig config) throws InitializationException, InstantiationException, InterruptedException {
        UnitRemote unitRemote = newInstance(config);
        unitRemote.init(config);
        return unitRemote;
    }

    /**
     * {@inheritDoc}
     *
     * @param scope {@inheritDoc}
     * @param type {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public UnitRemote newInitializedInstance(final Scope scope, final UnitType type) throws InitializationException, InstantiationException, InterruptedException {
        UnitRemote unitRemote = newInstance(type);
        unitRemote.init(scope);
        return unitRemote;
    }

    /**
     * {@inheritDoc}
     *
     * @param scope {@inheritDoc}
     * @param unitRemoteClass {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public <R extends AbstractUnitRemote> R newInitializedInstance(final Scope scope, final Class<R> unitRemoteClass) throws InitializationException, InstantiationException, InterruptedException {
        R unitRemote = newInstance(unitRemoteClass);
        unitRemote.init(scope);
        return unitRemote;
    }

    /**
     * {@inheritDoc}
     *
     * @param scope {@inheritDoc}
     * @param timeout {@inheritDoc}
     * @param timeUnit {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public UnitRemote newInitializedInstance(ScopeType.Scope scope, long timeout, TimeUnit timeUnit) throws InitializationException, InstantiationException, CouldNotPerformException, InterruptedException {
        CachedUnitRegistryRemote.waitForData(timeout, timeUnit);
        return newInitializedInstance(CachedUnitRegistryRemote.getRegistry().getUnitConfigByScope(scope));
    }

    /**
     * {@inheritDoc}
     *
     * @param unitId {@inheritDoc}
     * @param timeout {@inheritDoc}
     * @param timeUnit {@inheritDoc}
     * @return {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws InstantiationException {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public UnitRemote newInitializedInstance(final String unitId, long timeout, TimeUnit timeUnit) throws InitializationException, InstantiationException, CouldNotPerformException, InterruptedException {
        CachedUnitRegistryRemote.waitForData(timeout, timeUnit);
        return newInitializedInstance(CachedUnitRegistryRemote.getRegistry().getUnitConfigById(unitId));
    }
}
