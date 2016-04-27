/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.jul.exception.CouldNotPerformException;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface OpeningRatioStateOperationServiceCollection extends OpeningRatioService {

    @Override
    default public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        for (OpeningRatioService service : getOpeningRatioStateOperationServices()) {
            service.setOpeningRatio(openingRatio);
        }
    }

    /**
     * Returns the average opening ratio for a collection of opening ratio
     * services.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public Double getOpeningRatio() throws CouldNotPerformException {
        Double average = 0d;
        for (OpeningRatioService service : getOpeningRatioStateOperationServices()) {
            average += service.getOpeningRatio();
        }
        average /= getOpeningRatioStateOperationServices().size();
        return average;
    }

    public Collection<OpeningRatioService> getOpeningRatioStateOperationServices();
}
