package org.dc.bco.registry.mock;

/*
 * #%L
 * REM Utility
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.schedule.SyncObject;

/**
 *
 * @author mpohling
 */
public class MockRegistryHolder {

    private final static SyncObject mockRegistrySync = new SyncObject("MockRegistrySync");
    private static MockRegistry mockRegistry;

    public static MockRegistry newMockRegistry() throws InstantiationException {
        synchronized (mockRegistrySync) {
            if (mockRegistry != null) {
                throw new InstantiationException(MockRegistryHolder.class, new InvalidStateException("There is still one MockRegistry instance running!"));
            }

            mockRegistry = new MockRegistry();
            return mockRegistry;
        }
    }

    public static void shutdownMockRegistry() {
        synchronized (mockRegistrySync) {
            if (mockRegistry == null) {
                return;
            }
            mockRegistry.shutdown();
            mockRegistry = null;
        }
    }
}
