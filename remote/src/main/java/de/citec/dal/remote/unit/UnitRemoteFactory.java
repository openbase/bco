/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.processing.StringProcessor;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * A unit remote factory which can be used to create unit remote instances out
 * of device- and location-manager delivered unit configurations.
 *
 * @author mpohling
 */
public class UnitRemoteFactory implements UnitRemoteFactoryInterface {

    private static UnitRemoteFactoryInterface instance;

    private UnitRemoteFactory() {
    }

    public synchronized static UnitRemoteFactoryInterface getInstance() {
        if (instance == null) {
            instance = new UnitRemoteFactory();
        }
        return instance;
    }

    /**
     * Creates an unit remote out of the given unit configuration.
     *
     * @param config the unit configuration which defines the remote type.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     */
    @Override
    public DALRemoteService createUnitRemote(final UnitConfigType.UnitConfig config) throws CouldNotPerformException {
        try {
            return instantiatUnitRemote(loadUnitRemoteClass(config));
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not create unit remote!", ex);
        }
    }

    /**
     * Creates and initializes an unit remote out of the given unit
     * configuration.
     *
     * @param config the unit configuration which defines the remote type and is
     * used for the remote initialization.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     */
    @Override
    public DALRemoteService createAndInitUnitRemote(final UnitConfigType.UnitConfig config) throws CouldNotPerformException {
        DALRemoteService unitRemote = createUnitRemote(config);
        unitRemote.init(config);
        return unitRemote;
    }

    public static Class<? extends DALRemoteService> loadUnitRemoteClass(final UnitConfigType.UnitConfig config) throws CouldNotPerformException {
        return loadUnitRemoteClass(config.getType());
    }
    
    public static Class<? extends DALRemoteService> loadUnitRemoteClass(final UnitType unitType) throws CouldNotPerformException {
        String remoteClassName = DALRemoteService.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + "Remote";
        try {
            return (Class<? extends DALRemoteService>) UnitRemoteFactory.class.getClassLoader().loadClass(remoteClassName);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not detect unit remote class for UnitType[" + unitType.name() + "]!", ex);
        }
    }

    private static DALRemoteService instantiatUnitRemote(final Class<? extends DALRemoteService> unitRemoteClass) throws de.citec.jul.exception.InstantiationException {
        try {
            DALRemoteService instance = unitRemoteClass.newInstance();
            return instance;
        } catch (Exception ex) {
            throw new de.citec.jul.exception.InstantiationException("Could not instantiate unit remote out of Class[" + unitRemoteClass.getName() + "]", ex);
        }
    }
}
