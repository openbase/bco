/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.DimService;
import org.dc.jul.exception.CouldNotPerformException;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface DimStateOperationServiceCollection extends DimService {

    @Override
    default public void setDim(Double dim) throws CouldNotPerformException {
        for (DimService service : getDimStateOperationServices()) {
            service.setDim(dim);
        }
    }

    /**
     * Returns the average dim value for a collection of dim services.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public Double getDim() throws CouldNotPerformException {
        Double average = 0d;
        for (DimService service : getDimStateOperationServices()) {
            average += service.getDim();
        }
        average /= getDimStateOperationServices().size();
        return average;
    }

    public Collection<DimService> getDimStateOperationServices();
}
