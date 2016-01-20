/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import java.util.Collection;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ServiceRemoteFactoryImpl implements ServiceRemoteFactory {

    private static ServiceRemoteFactory instance;

    private ServiceRemoteFactoryImpl() {
    }

    public synchronized static ServiceRemoteFactory getInstance() {
        if (instance == null) {
            instance = new ServiceRemoteFactoryImpl();
        }
        return instance;
    }

    @Override
    public AbstractServiceRemote createAndInitServiceRemote(ServiceType serviceType, Collection<UnitConfig> unitConfigs) throws CouldNotPerformException {
        AbstractServiceRemote serviceRemote = createServiceRemote(serviceType);
        serviceRemote.init(unitConfigs);
        return serviceRemote;
    }

    @Override
    public AbstractServiceRemote createAndInitServiceRemote(ServiceType serviceType, UnitConfig unitConfig) throws CouldNotPerformException {
        AbstractServiceRemote serviceRemote = createServiceRemote(serviceType);
        serviceRemote.init(unitConfig);
        return serviceRemote;
    }

    @Override
    public AbstractServiceRemote createServiceRemote(ServiceType serviceType) throws CouldNotPerformException {
        try {
            return instantiatServiceRemote(loadServiceRemoteClass(serviceType));
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not create service remote!", ex);
        }
    }

    public static Class<? extends AbstractServiceRemote> loadServiceRemoteClass(final ServiceType serviceType) throws CouldNotPerformException {
        String remoteClassName = AbstractServiceRemote.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(serviceType.name()) + "Remote";
        try {
            return (Class<? extends AbstractServiceRemote>) ServiceRemoteFactoryImpl.class.getClassLoader().loadClass(remoteClassName);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not detect service remote class for ServiceType[" + serviceType.name() + "]!", ex);
        }
    }

    private static AbstractServiceRemote instantiatServiceRemote(final Class<? extends AbstractServiceRemote> serviceRemoteClass) throws org.dc.jul.exception.InstantiationException {
        try {
            AbstractServiceRemote remote = serviceRemoteClass.newInstance();
            return remote;
        } catch (Exception ex) {
            throw new org.dc.jul.exception.InstantiationException("Could not instantiate service remote out of Class[" + serviceRemoteClass.getName() + "]", ex);
        }
    }
}
