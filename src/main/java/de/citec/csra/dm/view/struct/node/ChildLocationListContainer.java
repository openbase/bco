/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import java.util.Collection;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author thuxohl
 */
public class ChildLocationListContainer extends NodeContainer<Collection<LocationConfig>> {

    public ChildLocationListContainer(final Collection<LocationConfig> locationConfigs) {
        super("Child Locations", locationConfigs);
        locationConfigs.stream().forEach((locationConfig) -> {
            super.add(new LocationConfigContainer(locationConfig));
        });
    }
}
