/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import de.citec.jul.exception.InstantiationException;
import de.citec.jul.schedule.SyncObject;

/**
 *
 * @author mpohling
 */
public class MockFactory {

    private static int mockRegistryCounter = 0;
    private final static SyncObject mockRegistrySync = new SyncObject("MockRegistrySync");
    private static MockRegistry mockRegistry;

    public static MockRegistry newMockRegistry() throws InstantiationException {
        synchronized (mockRegistrySync) {
            if (mockRegistry == null) {
                assert mockRegistryCounter == 0;
                mockRegistry = new MockRegistry();
            }
            mockRegistryCounter++;
            assert mockRegistry != null;
            return mockRegistry;
        }
    }

    public static void shutdownMockRegistry() {
        synchronized (mockRegistrySync) {
            if(mockRegistry == null) {
                assert mockRegistryCounter == 0;
                return;
            }
            mockRegistryCounter--;
            
            if(mockRegistryCounter == 0) {
                mockRegistry.shutdown();
                mockRegistry = null;
            }
        }
    }
}
