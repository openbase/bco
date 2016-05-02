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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.dc.bco.dal.lib.layer.service.operation.OpeningRatioOperationService;
import org.dc.jul.exception.CouldNotPerformException;
=======
import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.bco.dal.lib.layer.service.collection.OpeningRatioStateOperationServiceCollection;
>>>>>>> master
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
<<<<<<< HEAD
public class OpeningRatioServiceRemote extends AbstractServiceRemote<OpeningRatioOperationService> implements OpeningRatioOperationService {
=======
public class OpeningRatioServiceRemote extends AbstractServiceRemote<OpeningRatioService> implements OpeningRatioStateOperationServiceCollection {
>>>>>>> master

    public OpeningRatioServiceRemote() {
        super(ServiceType.OPENING_RATIO_SERVICE);
    }

    @Override
<<<<<<< HEAD
    public Future<Void> setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        List<Future> futureList = new ArrayList<>();
        for (OpeningRatioOperationService service : getServices()) {
            futureList.add(service.setOpeningRatio(openingRatio));
        }
        return Future.allOf(futureList.toArray(new Future[futureList.size()]));
    }

    /**
     * Returns the average opening ratio for a collection of opening ratio
     * services.
     *
     * @return
     * @throws CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    @Override
    public Double getOpeningRatio() throws CouldNotPerformException, InterruptedException {
        Double average = 0d;
        for (OpeningRatioOperationService service : getServices()) {
            average += service.getOpeningRatio();
        }
        average /= getServices().size();
        return average;
=======
    public Collection<OpeningRatioService> getOpeningRatioStateOperationServices() {
        return getServices();
>>>>>>> master
    }
}
