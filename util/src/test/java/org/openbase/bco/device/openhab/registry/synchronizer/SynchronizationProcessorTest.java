package org.openbase.bco.device.openhab.registry.synchronizer;

/*-
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.junit.Assert;

import static org.junit.jupiter.api.Assertions.*;

class SynchronizationProcessorTest {

    @org.junit.jupiter.api.Test
    void getUniquePrefix() {
        Assert.assertNotEquals(
                "Prefix is not unique!",
                SynchronizationProcessor.getUniquePrefix("00:17:88:01:08:0c:f4:60-02-fc00"),
                SynchronizationProcessor.getUniquePrefix("00:17:88:01:08:70:3c:a9-02-fc00")

        );

        Assert.assertEquals(
                "Prefix is not unique!",
                SynchronizationProcessor.getUniquePrefix("00:17:88:01:08:0c:f4:60-02-fc00"),
                SynchronizationProcessor.getUniquePrefix("00:17:88:01:08:0c:f4:60-02-fc22")

        );


    }
}
