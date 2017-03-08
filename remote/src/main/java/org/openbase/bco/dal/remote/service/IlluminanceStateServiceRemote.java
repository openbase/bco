/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.dal.remote.service;

import java.util.Collection;
import org.openbase.bco.dal.lib.layer.service.collection.IlluminanceStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.bco.dal.lib.layer.service.provider.IlluminanceStateProviderService;

/**
 *
 * @author pleminoq
 */
public class IlluminanceStateServiceRemote extends AbstractServiceRemote<IlluminanceStateProviderService, IlluminanceState> implements IlluminanceStateProviderServiceCollection {
    
    public IlluminanceStateServiceRemote() {
        super(ServiceType.ILLUMINANCE_STATE_SERVICE);
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
        
        return TimestampProcessor.updateTimestamp(timestamp, IlluminanceState.newBuilder().setIlluminance(averageIlluminance).setIlluminanceDataUnit(IlluminanceState.DataUnit.LUX), logger).build();
    }
    
}
