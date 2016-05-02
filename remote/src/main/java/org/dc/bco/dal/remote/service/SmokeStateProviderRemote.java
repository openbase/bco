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

import org.dc.bco.dal.lib.layer.service.provider.SmokeStateProviderService;
import org.dc.jul.exception.CouldNotPerformException;
=======
import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.collection.SmokeStateProviderServiceCollection;
import org.dc.bco.dal.lib.layer.service.provider.SmokeStateProvider;
>>>>>>> master
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
<<<<<<< HEAD
public class SmokeStateProviderRemote extends AbstractServiceRemote<SmokeStateProviderService> implements SmokeStateProviderService {
=======
public class SmokeStateProviderRemote extends AbstractServiceRemote<SmokeStateProvider> implements SmokeStateProviderServiceCollection {
>>>>>>> master

    public SmokeStateProviderRemote() {
        super(ServiceType.SMOKE_STATE_PROVIDER);
    }

<<<<<<< HEAD
    /**
     * If at least one smoke state provider returns smoke than that is returned.
     * Else if at least one returns some smoke than that is returned. Else no
     * smoke is returned.
     *
     * @return
     * @throws CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    @Override
    public SmokeState getSmokeState() throws CouldNotPerformException, InterruptedException {
        boolean someSmoke = false;
        for (SmokeStateProviderService provider : getServices()) {
            if (provider.getSmokeState().getValue() == SmokeState.State.SMOKE) {
                return SmokeState.newBuilder().setValue(SmokeState.State.SMOKE).build();
            }
            if (provider.getSmokeState().getValue() == SmokeState.State.SOME_SMOKE) {
                someSmoke = true;
            }
        }
        if (someSmoke) {
            return SmokeState.newBuilder().setValue(SmokeState.State.SOME_SMOKE).build();
        } else {
            return SmokeState.newBuilder().setValue(SmokeState.State.NO_SMOKE).build();
        }
=======
    @Override
    public Collection<SmokeStateProvider> getSmokeStateProviderServices() {
        return getServices();
>>>>>>> master
    }
}
