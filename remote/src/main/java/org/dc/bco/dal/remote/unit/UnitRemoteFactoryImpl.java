package org.dc.bco.dal.remote.unit;

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
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * A unit remote factory which can be used to create unit remote instances out
 * of device- and location-manager delivered unit configurations.
 *
 * @author mpohling
 */
public class UnitRemoteFactoryImpl implements UnitRemoteFactory {

    private static UnitRemoteFactory instance;

    private UnitRemoteFactoryImpl() {
    }

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
     * @throws org.dc.jul.exception.InitializationException
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

    public static Class<? extends AbstractUnitRemote> loadUnitRemoteClass(final UnitConfig config) throws CouldNotPerformException {
        return loadUnitRemoteClass(config.getType());
    }

    public static Class<? extends AbstractUnitRemote> loadUnitRemoteClass(final UnitType unitType) throws CouldNotPerformException {
        String remoteClassName = AbstractUnitRemote.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + "Remote";
        try {
            return (Class<? extends AbstractUnitRemote>) UnitRemoteFactoryImpl.class.getClassLoader().loadClass(remoteClassName);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not detect unit remote class for UnitType[" + unitType.name() + "]!", ex);
        }
    }

    private static AbstractUnitRemote instantiatUnitRemote(final Class<? extends AbstractUnitRemote> unitRemoteClass) throws org.dc.jul.exception.InstantiationException {
        try {
            AbstractUnitRemote remote = unitRemoteClass.newInstance();
            return remote;
        } catch (java.lang.InstantiationException | IllegalAccessException ex) {
            throw new org.dc.jul.exception.InstantiationException("Could not instantiate unit remote out of Class[" + unitRemoteClass.getName() + "]", ex);
        }
    }

    @Override
    public UnitRemote newInitializedInstance(final UnitConfig config) throws InitializationException, InstantiationException, InterruptedException {
        UnitRemote unitRemote = newInstance(config);
        unitRemote.init(config);
        return unitRemote;
    }

    @Override
    public UnitRemote newInstance(final UnitConfig config) throws InstantiationException {
        try {
            return instantiatUnitRemote(loadUnitRemoteClass(config));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException("Could not create unit remote!", ex);
        }
    }
}
