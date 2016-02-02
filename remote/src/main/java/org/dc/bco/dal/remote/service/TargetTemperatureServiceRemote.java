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

import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class TargetTemperatureServiceRemote extends AbstractServiceRemote<TargetTemperatureService> implements TargetTemperatureService {

    public TargetTemperatureServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    public void setTargetTemperature(Double value) throws CouldNotPerformException {
        for (TargetTemperatureService service : getServices()) {
            service.setTargetTemperature(value);
        }
    }

    @Override
    public Double getTargetTemperature() throws CouldNotPerformException {
        throw new CouldNotPerformException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
