/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

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
<<<<<<< HEAD
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.dc.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.dc.jul.exception.CouldNotPerformException;
=======
import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.bco.dal.lib.layer.service.collection.PowerStateOperationServiceCollection;
>>>>>>> master
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author mpohling
 */
<<<<<<< HEAD
public class PowerServiceRemote extends AbstractServiceRemote<PowerOperationService> implements PowerOperationService {
=======
public class PowerServiceRemote extends AbstractServiceRemote<PowerService> implements PowerStateOperationServiceCollection {
>>>>>>> master

    public PowerServiceRemote() {
        super(ServiceType.POWER_SERVICE);
    }

    @Override
<<<<<<< HEAD
    public Future<Void> setPower(final PowerStateType.PowerState state) throws CouldNotPerformException {
        List<Future> futureList = new ArrayList<>();
        for (PowerOperationService service : getServices()) {
            futureList.add(service.setPower(state));
        }
        return Future.allOf(futureList.toArray(new Future[futureList.size()]));
    }

    /**
     * Returns on if at least one of the power services is on and else off.
     *
     * @return
     * @throws CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    @Override
    public PowerStateType.PowerState getPower() throws CouldNotPerformException, InterruptedException {
        for (PowerOperationService service : getServices()) {
            if (service.getPower().getValue() == PowerState.State.ON) {
                return PowerStateType.PowerState.newBuilder().setValue(PowerState.State.ON).build();
            }
        }
        return PowerStateType.PowerState.newBuilder().setValue(PowerState.State.OFF).build();
=======
    public Collection<PowerService> getPowerStateOperationServices() {
        return getServices();
>>>>>>> master
    }
}
