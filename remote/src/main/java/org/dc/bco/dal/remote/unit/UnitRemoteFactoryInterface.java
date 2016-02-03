/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import org.dc.jul.extension.rsb.com.AbstractIdentifiableRemote;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public interface UnitRemoteFactoryInterface {

    /**
     * Creates and initializes an unit remote out of the given unit configuration.
     * @param config the unit configuration which defines the remote type and is used for the remote initialization.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     */
    public AbstractIdentifiableRemote createAndInitUnitRemote(final UnitConfigType.UnitConfig config) throws CouldNotPerformException;

    /**
     * Creates an unit remote out of the given unit configuration.
     * @param config the unit configuration which defines the remote type.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     */
    public AbstractIdentifiableRemote createUnitRemote(final UnitConfigType.UnitConfig config) throws CouldNotPerformException;
    
}
