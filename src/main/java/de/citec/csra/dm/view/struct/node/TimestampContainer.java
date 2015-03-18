/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.node;

import rst.timing.TimestampType.Timestamp;

/**
 *
 * @author thuxohl
 */
public class TimestampContainer extends NodeContainer<Timestamp.Builder> {

    public TimestampContainer(Timestamp.Builder timestamp) {
        super("Timestamp", timestamp);
        super.add(timestamp.getTime(), "time");
    }
}
