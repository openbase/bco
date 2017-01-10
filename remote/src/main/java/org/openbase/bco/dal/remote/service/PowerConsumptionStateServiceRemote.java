package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.Collection;
import org.openbase.bco.dal.lib.layer.service.collection.PowerConsumptionStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.PowerConsumptionStateProviderService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PowerConsumptionStateServiceRemote extends AbstractServiceRemote<PowerConsumptionStateProviderService, PowerConsumptionState> implements PowerConsumptionStateProviderServiceCollection {

    public PowerConsumptionStateServiceRemote() {
        super(ServiceType.POWER_CONSUMPTION_STATE_SERVICE);
    }

    @Override
    public Collection<PowerConsumptionStateProviderService> getPowerConsumptionStateProviderServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the average current and voltage and the sum of the consumption of the underlying services.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected PowerConsumptionState computeServiceState() throws CouldNotPerformException {
        double consumptionSum = 0;
        double averageCurrent = 0;
        double averageVoltage = 0;
        Collection<PowerConsumptionStateProviderService> powerConsumptionStateProviderServices = getPowerConsumptionStateProviderServices();
        int amount = powerConsumptionStateProviderServices.size();
        for (PowerConsumptionStateProviderService provider : powerConsumptionStateProviderServices) {
            if (!((UnitRemote) provider).isDataAvailable()) {
                amount--;
                continue;
            }

            consumptionSum += provider.getPowerConsumptionState().getConsumption();
            averageCurrent += provider.getPowerConsumptionState().getCurrent();
            averageVoltage += provider.getPowerConsumptionState().getVoltage();
        }
        averageCurrent = averageCurrent / amount;
        averageVoltage = averageVoltage / amount;

        return PowerConsumptionState.newBuilder().setConsumption(consumptionSum).setCurrent(averageCurrent).setVoltage(averageVoltage).setTimestamp(Timestamp.newBuilder().setTime(System.currentTimeMillis())).build();
    }

    @Override
    public PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        return getServiceState();
    }
}
