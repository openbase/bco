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

import org.dc.bco.dal.lib.layer.service.provider.SmokeAlarmStateProviderService;
import org.dc.jul.exception.CouldNotPerformException;
=======
import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.collection.SmokeAlarmStateProviderServiceCollection;
import org.dc.bco.dal.lib.layer.service.provider.SmokeAlarmStateProvider;
>>>>>>> master
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
<<<<<<< HEAD
public class SmokeAlarmStateProviderRemote extends AbstractServiceRemote<SmokeAlarmStateProviderService> implements SmokeAlarmStateProviderService {
=======
public class SmokeAlarmStateProviderRemote extends AbstractServiceRemote<SmokeAlarmStateProvider> implements SmokeAlarmStateProviderServiceCollection {
>>>>>>> master

    public SmokeAlarmStateProviderRemote() {
        super(ServiceType.SMOKE_ALARM_STATE_PROVIDER);
    }

<<<<<<< HEAD
    /**
     * Returns alarm if at least one smoke alarm state provider returns alarm
     * else no alarm.
     *
     * @return
     * @throws CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    @Override
    public AlarmState getSmokeAlarmState() throws CouldNotPerformException, InterruptedException {
        for (SmokeAlarmStateProviderService provider : getServices()) {
            if (provider.getSmokeAlarmState().getValue() == AlarmState.State.ALARM) {
                return AlarmState.newBuilder().setValue(AlarmState.State.ALARM).build();
            }
        }
        return AlarmState.newBuilder().setValue(AlarmState.State.NO_ALARM).build();
=======
    @Override
    public Collection<SmokeAlarmStateProvider> getSmokeAlarmStateProviderServices() {
        return getServices();
>>>>>>> master
    }
}
