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

import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class BrightnessServiceRemote extends AbstractServiceRemote<BrightnessService> implements BrightnessService {

    public BrightnessServiceRemote() {
        super(ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_SERVICE);
    }

    @Override
    public void setBrightness(Double brightness) throws CouldNotPerformException {
        for (BrightnessService service : getServices()) {
            service.setBrightness(brightness);
        }
    }

    /**
     * Returns the average brightness value for a collection of brightness
     * services.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Double getBrightness() throws CouldNotPerformException {
        Double average = 0d;
        for (BrightnessService service : getServices()) {
            average += service.getBrightness();
        }
        average /= getServices().size();
        return average;
    }
}
