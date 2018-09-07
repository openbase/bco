package org.openbase.bco.app.openhab;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class KNXTransferer {

    private static final String BINDING_ID = "knx";
    private static final String THING_TYPE = "device";
    private static final String BRIDGE_ID = "12345";

    public static void main(String[] args) {
        //TODO: finde bridg: iterate over things whose id start with knx, find the ones with BridgeImpl class
        final ThingDTO thingDTO = new ThingDTO();
        final ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, THING_TYPE);
        // bridge uid muss nicht teil der thinguid sein aber separat gesetzt sein...
        final ThingUID thingUID = new ThingUID(thingTypeUID, "ads"); //TODO: get id from bco

        thingDTO.bridgeUID = BRIDGE_ID;
        thingDTO.thingTypeUID = thingTypeUID.toString();
        thingDTO.UID = thingTypeUID.getId();

        //NOTE: in configuration {
        //  properties {
        //      ga : =0/1/1
        //  }
        //}
        ChannelDTO channelDTO = new ChannelDTO();
//        OpenHABRestCommunicator.getInstance().registerThing()


    }
}
