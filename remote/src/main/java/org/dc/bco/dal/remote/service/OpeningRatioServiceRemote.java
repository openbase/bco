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

import org.dc.bco.dal.lib.layer.service.operation.OpeningRatioOperationService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class OpeningRatioServiceRemote extends AbstractServiceRemote<OpeningRatioOperationService> implements OpeningRatioOperationService{

    public OpeningRatioServiceRemote() {
        super(ServiceType.OPENING_RATIO_SERVICE);
    }

    @Override
    public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        for(OpeningRatioOperationService service : getServices()) {
        for (OpeningRatioService service : getServices()) {
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
    public Double getOpeningRatio() throws CouldNotPerformException {
        Double average = 0d;
        for (OpeningRatioService service : getServices()) {
            average += service.getOpeningRatio();
        }
        average /= getServices().size();
        return average;
    }
}
