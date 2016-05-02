/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
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
