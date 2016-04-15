/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.location.lib;

import org.dc.jul.pattern.Factory;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface LocationFactory extends Factory<Location, LocationConfig> {

    @Override
    public LocationController newInstance(final LocationConfig config) throws org.dc.jul.exception.InstantiationException;
}
