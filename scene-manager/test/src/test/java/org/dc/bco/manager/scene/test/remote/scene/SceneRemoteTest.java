/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.test.remote.scene;

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
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rsb.Factory;
import rsb.Informer;
import rsb.config.ParticipantConfig;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ButtonStateType.ButtonState;
import rst.homeautomation.unit.ButtonType.Button;

/**
 *
 * @author thuxohl
 */
public class SceneRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SceneRemoteTest.class);

    public SceneRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, InstantiationException, CouldNotPerformException, JPServiceException, InterruptedException {
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {

    }

    /**
     *  Used to test the activation of a scene via a button
     * @throws java.lang.Exception
     */
//    @Test
    public void testScene() throws Exception {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Button.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ButtonState.getDefaultInstance()));

        ParticipantConfig config = Factory.getInstance().getDefaultParticipantConfig();
        final String scope = "/home/sports/button/pathwaybelowbutton_3/status";

        Informer<Button> informer = Factory.getInstance().createInformer(scope, config);
        informer.activate();

        Button button = Button.newBuilder().setButtonState(ButtonState.newBuilder().setValue(ButtonState.State.CLICKED).build()).build();
        informer.send(button);
    }
}
