/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author thuxohl
 */
public class LocationConfigContainer extends NodeContainer<LocationConfig> {

    public LocationConfigContainer(LocationConfig location) {
        super("Location", location);
        super.add(location.getLabel(), "Label");
        super.add(location.getRoot(), "Root Location");
        super.add(new ScopeContainer(location.getScope()));
        super.add(new ChildLocationListContainer(location.getChildrenList()));
        if (location.hasParent()) {
            super.add(location.getParent().getLabel(), "Parent Label");
        }
        //TODO: change the location type to avoid the infite loop!
    }
}
