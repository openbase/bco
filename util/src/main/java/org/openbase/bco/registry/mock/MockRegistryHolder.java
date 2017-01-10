package org.openbase.bco.registry.mock;

/*
 * #%L
 * BCO Registry Utility
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.schedule.SyncObject;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
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
