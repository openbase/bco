/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.control.action;

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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.dc.bco.dal.remote.service.AbstractServiceRemote;
import org.dc.bco.dal.remote.service.ServiceRemoteFactory;
import org.dc.bco.dal.remote.service.ServiceRemoteFactoryImpl;
import org.dc.bco.registry.device.lib.DeviceRegistry;
import org.dc.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.iface.Initializable;
import org.dc.jul.schedule.SyncObject;
import rst.homeautomation.control.action.ActionConfigType;
import rst.homeautomation.control.action.ActionConfigType.ActionConfig;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class Action implements ActionService, Initializable<ActionConfig> {

    private ActionConfig config;
    private ServiceRemoteFactory serviceRemoteFactory;
    private DeviceRegistry deviceRegistry;
    private AbstractServiceRemote serviceRemote;

    public Action() {
    }

    @Override
    public void init(final ActionConfigType.ActionConfig config) throws InitializationException, InterruptedException {
        try {
            this.config = config;
            this.deviceRegistry = CachedDeviceRegistryRemote.getDeviceRegistry();
            this.serviceRemoteFactory = ServiceRemoteFactoryImpl.getInstance();
            this.serviceRemote = serviceRemoteFactory.createAndInitServiceRemote(config.getServiceType(), deviceRegistry.getUnitConfigById(config.getServiceHolder()));
            serviceRemote.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private Future executionFuture;
    private final SyncObject executionSync = new SyncObject(Action.class);
    
    @Override
    public void execute() throws CouldNotPerformException {
        executionFuture = new FutureTask(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
//                serviceRemote.applyAction(Action.this);
                return null;
            }
        });
        
    }

    public void waitForFinalization() {
        //TODO
    }

    public ActionConfig getConfig() {
        return config;
    }
}
