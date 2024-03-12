---
---
# BCO User Documentation

## Initial Setup

1. [Install BCO Runtime](/user/installation.md)
2. Install BCozy on your desktop pc
3. Start BCozy and login:
   * Use the default credentials (User:```admin``` Password:```admin```) for the initial login.
   * Afterwards an assistant guides you through the initial setup (not implemented yet).

## BCozy (Desktop Application)
[![GUI Overview](/img/bco/bcozy/bcozy_gui_overview.png)](bcozy.md)
Managing your devices in a menu structure is a well known approach but can end up really bad when dealing with too many entries and levels.
Since inhabitants are perfectly familiar with the physical structure of their environment we thought about using such a representation for management and control purposes.
Therefore, we developed BCozy to organize and control your smart environment via a 2D map.
[More Details about BCozy](bcozy.md)

## BComfy (Android App)
[![GUI Overview](/img/bco/bcomfy/2_interact_3.jpg)](bcomfy.md)
This Android app can be used to create a 3D representation of your environment.
When scanning your rooms and your devices, their shape and arrangement are stored within the bco database.
Afterwards this information is used as the baseline for automation routines and visualization purposes.
Once the 3D information is available, BComfy augments the smartphone camera view with virtual control elements.
This augmented reality interface is especially useful to maintain your environment.
Checkout battery levels and sensor events in realtime while walking through your home.
[More Details about BComfy](bcomfy.md)

:::info INFO
Because the augmented reality framework is build on top of Google Tango, BCozy is currently limited to Android devices with Tango support. Developers with knowledge about AR Core are highly welcome to join our taskforce to port BComfy to the AR Core Framework.  
:::
