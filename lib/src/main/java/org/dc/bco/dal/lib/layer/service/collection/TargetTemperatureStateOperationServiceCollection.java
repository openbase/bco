/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.jul.exception.CouldNotPerformException;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface TargetTemperatureStateOperationServiceCollection extends TargetTemperatureService {

    @Override
    default public void setTargetTemperature(Double value) throws CouldNotPerformException {
        for (TargetTemperatureService service : getTargetTemperatureStateOperationServices()) {
            service.setTargetTemperature(value);
        }
    }

    /**
     * Returns the average target temperature value for a collection of target
     * temperature services.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public Double getTargetTemperature() throws CouldNotPerformException {
        Double average = 0d;
        for (TargetTemperatureService service : getTargetTemperatureStateOperationServices()) {
            average += service.getTargetTemperature();
        }
        average /= getTargetTemperatureStateOperationServices().size();
        return average;
    }

    public Collection<TargetTemperatureService> getTargetTemperatureStateOperationServices();
}
