package org.openbase.bco.registry.location.test;

/*-
 * #%L
 * BCO Registry Location Test
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.launch.RegistryLauncher;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.registry.LocationRegistryDataType.LocationRegistryData;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class TestRemoteLocationRegistrySync {

    private final String absoluteDBPath = "/homes/thuxohl/release/local/var/bco/registry/db";

    @BeforeClass
    public static void setUpClass() throws Exception {
        JPService.setupJUnitTestMode();
    }

    //@Test(timeout = 120000)
    public void testInitialDBSync() throws Exception {
        GlobalCachedExecutorService.submit(this::startRegistry);

        Registries.getLocationRegistry().addDataObserver((Observable<LocationRegistryData> source, LocationRegistryData data) -> {
            System.out.println("LocationRegistryUpdate with " + data.getLocationUnitConfigCount()+ " locations!");
        });
        Registries.getLocationRegistry().waitForData();
        System.out.println("WaitForData has returned with " + Registries.getLocationRegistry().getData().getLocationUnitConfigCount() + " locations");
        
        Assert.assertTrue("LocationRegistry contains 0 locations after waitForData", Registries.getLocationRegistry().getData().getLocationUnitConfigCount() > 0);
        
        try {
            Registries.getLocationRegistry().getUnitConfigsByLocation(Registries.getLocationRegistry().getRootLocationConfig().getId());
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Failed even though waitForDataReturned", ex);
        }
    }

    public Void startRegistry() {
        String[] args = new String[2];
        args[0] = "--database";
        args[1] = absoluteDBPath;

        RegistryLauncher.main(args);

        return null;
    }

}
