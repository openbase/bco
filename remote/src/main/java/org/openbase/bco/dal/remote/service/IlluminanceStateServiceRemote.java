/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.dal.remote.service;

/*-
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
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.collection.IlluminanceStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.IlluminanceStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author pleminoq
 */
public class IlluminanceStateServiceRemote extends AbstractServiceRemote<IlluminanceStateProviderService, IlluminanceState> implements IlluminanceStateProviderServiceCollection {

    public IlluminanceStateServiceRemote() {
        super(ServiceType.ILLUMINANCE_STATE_SERVICE, IlluminanceState.class);
    }

    public Collection<IlluminanceStateProviderService> getIlluminanceStateProviderServices() {
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
    protected IlluminanceState computeServiceState() throws CouldNotPerformException {
        return getIlluminanceState(UnitType.UNKNOWN);
    }

    @Override
    public IlluminanceState getIlluminanceState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public IlluminanceState getIlluminanceState(final UnitType unitType) throws NotAvailableException {
        double averageIlluminance = 0;
        long timestamp = 0;
        Collection<IlluminanceStateProviderService> illuminanceStateProviderServices = getServices(unitType);
        int amount = illuminanceStateProviderServices.size();
        for (IlluminanceStateProviderService service : illuminanceStateProviderServices) {
            if (!((UnitRemote) service).isDataAvailable()) {
                amount--;
                continue;
            }

            averageIlluminance += Math.max(timestamp, service.getIlluminanceState().getTimestamp().getTime());
        }
        averageIlluminance = averageIlluminance / amount;

        return TimestampProcessor.updateTimestamp(timestamp, IlluminanceState.newBuilder().setIlluminance(averageIlluminance).setIlluminanceDataUnit(IlluminanceState.DataUnit.LUX), TimeUnit.MICROSECONDS, logger).build();
    }

}
