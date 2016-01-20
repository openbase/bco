/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import java.util.Collection;
import org.dc.bco.dal.remote.unit.DALRemoteService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface ServiceRemoteFactory {

    /**
     * Creates and initializes a service remote out of the given service type
     * and a collection of unitConfigs.
     *
     * @param serviceType
     * @param unitConfigs
     * @return the new created service remote.
     * @throws CouldNotPerformException
     */
    public AbstractServiceRemote createAndInitServiceRemote(final ServiceType serviceType, final Collection<UnitConfig> unitConfigs) throws CouldNotPerformException;

    /**
     * Creates and initializes a service remote out of the given service type
     * and unitConfig.
     *
     * @param serviceType
     * @param unitConfig
     * @return the new created service remote.
     * @throws CouldNotPerformException
     */
    public AbstractServiceRemote createAndInitServiceRemote(final ServiceType serviceType, final UnitConfig unitConfig) throws CouldNotPerformException;

    /**
     * Creates a service remote out of the given service type.
     *
     * @param serviceType
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     */
    public AbstractServiceRemote createServiceRemote(final ServiceType serviceType) throws CouldNotPerformException;
}
