---
title: BCO - Location Architecture
permalink: "/location-architecture/"
layout: default
---

# Location Architecture

![LocationClassStructure]({{ site.baseurl }}/images/LocationArchitecture_ClassStructure.png)

![LocationHierarchicalStructure]({{ site.baseurl }}/images/LocationArchitecture_HierarchicalStructure.png)

![LocationGraphStructure]({{ site.baseurl }}/images/LocationArchitecture_GraphStructure.png)

## Modifications

### Removal

* The root location cannot be removed
* Removing a location causes every unit to be moved to its parent
* Removing a tile causes two additional effects:
   * Regions inside the tile are removed recursively
   * Every connection connecting this tile and only one other tile is removed

## Guidelines

* The transformation provided by each unit transforms between the unit and its parent location.
    * parent location: ```unit_config -> placement_config -> location_id```
* Regions are translated but not rotated within a tile.
* The position of a unit is anchored to the base of its 3d bounding box.
* Bounding box base should be aligned to the unit position which means the bounding box offset (left_front_bottom) should not be used!
