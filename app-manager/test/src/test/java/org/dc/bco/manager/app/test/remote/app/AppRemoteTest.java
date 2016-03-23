/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.app.test.remote.app;

/*
 * #%L
 * COMA DeviceManager Test
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.jps.exception.JPServiceException;
import org.dc.bco.manager.app.core.AppManagerLauncher;
import org.dc.bco.manager.app.remote.AppRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.bco.registry.mock.MockRegistry;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.extension.rsb.com.RSBFactory;
import org.dc.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.dc.jul.extension.rsb.iface.RSBInformerInterface;
import org.dc.jul.extension.rsb.iface.RSBListenerInterface;
import org.dc.jul.schedule.WatchDog;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rsb.Factory;
import rsb.Informer;
import rsb.Scope;
import rsb.config.ParticipantConfig;
import rsb.config.TransportConfig;

/**
 *
 * @author thuxohl
 */
public class AppRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AppRemoteTest.class);

    private static AppManagerLauncher appManagerLauncher;
    private static AppRemote appRemote;
    private static MockRegistry registry;

    public AppRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, JPServiceException, InterruptedException {
//        JPService.registerProperty(JPHardwareSimulationMode.class, true);
//        registry = MockRegistryHolder.newMockRegistry();
//
//        appManagerLauncher = new UserManagerLauncher();
//        appManagerLauncher.launch();

//        AppConfig appConfig = AppConfig.getDefaultInstance();
//        appRemote = new UserRemote();
//        appRemote.init(appConfig);
//        appRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
//        if (appManagerLauncher != null) {
//            appManagerLauncher.shutdown();
//        }
//        if (appRemote != null) {
//            appRemote.shutdown();
//        }
//        if (registry != null) {
//            MockRegistryHolder.shutdownMockRegistry();
//        }
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testSoundScapeApp() throws Exception {
        ParticipantConfig config = Factory.getInstance().getDefaultParticipantConfig();
        final String scope = "/app/soundscape/theme/";

        Informer<Object> informer = Factory.getInstance().createInformer(scope, config);
        informer.activate();
        informer.send("Beach");
    }
}
