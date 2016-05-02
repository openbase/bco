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
import org.dc.bco.dal.lib.layer.service.provider.PowerConsumptionProviderService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PowerConsumptionProviderRemote extends AbstractServiceRemote<PowerConsumptionProviderService> implements PowerConsumptionProviderService {

    public PowerConsumptionProviderRemote() {
        super(ServiceType.POWER_CONSUMPTION_PROVIDER);
    }

    /**
     * Returns an average current and voltage for the underlying provider and
     * the sum of their consumptions.
     *
     * @return
     * @throws CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    @Override
    public PowerConsumptionState getPowerConsumption() throws CouldNotPerformException, InterruptedException {
        double consumptionSum = 0;
        double averageCurrent = 0;
        double averageVoltage = 0;
        for (PowerConsumptionProviderService provider : getServices()) {
            consumptionSum += provider.getPowerConsumption().getConsumption();
            averageCurrent += provider.getPowerConsumption().getCurrent();
            averageVoltage += provider.getPowerConsumption().getVoltage();
        }
        averageCurrent = averageCurrent / getServices().size();
        averageVoltage = averageVoltage / getServices().size();
        return PowerConsumptionState.newBuilder().setConsumption(consumptionSum).setCurrent(averageCurrent).setVoltage(averageVoltage).build();
    }
}
