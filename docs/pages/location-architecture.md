---
layout: default
title: BCO - Location Architecture
permalink: /location-architecture/
---
# Location Architecture

![LocationClassStructure]({{ site.baseurl }}/images/LocationArchitecture_ClassStructure.png)

![LocationHierarchicalStructure]({{ site.baseurl }}/images/LocationArchitecture_HierarchicalStructure.png)

![LocationGraphStructure]({{ site.baseurl }}/images/LocationArchitecture_GraphStructure.png)

## Guidelines

* The transformation provided by each unit transforms between the unit and its parent location.
    * parent location: ```unit_config -> placement_config -> location_id```
* Regions are translated but not rotated within a tile.
* The position of a unit is anchored to the center of its 3d bounding box. 
