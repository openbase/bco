package org.openbase.bco.manager.location.test.remote.location;

/*
 * #%L
 * BCO Manager Location Test
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Remote;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ConnectionRemoteTest extends AbstractBCOLocationManagerTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LocationRemoteTest.class);

    private static ConnectionRemote connectionRemote;

    public ConnectionRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Throwable {
        try {
            AbstractBCOLocationManagerTest.setUpClass();

            connectionRemote = Units.getUnit(Registries.getLocationRegistry().getConnectionConfigs().get(0), true, ConnectionRemote.class);
            connectionRemote.waitForConnectionState(Remote.ConnectionState.CONNECTED);
        } catch (Throwable ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }

    @Before
    public void setUp() throws InitializationException, InvalidStateException {

    }

    @After
    public void tearDown() throws CouldNotPerformException {
    }

    /**
     * Test if changes in unitControllers are published to a connection remote.
     *
     * @throws Exception
     */
//    @Test(timeout = 5000)
//    public void testDoorStateUpdate() throws Exception {
//        System.out.println("testDoorStateUpdate");
//
//        List<ReedContactController> reedContactControllerList = new ArrayList<>();
//        for (UnitConfig dalUnitConfig : unitRegistry.getDalUnitConfigs()) {
//            UnitController unitController = deviceManagerLauncher.getLaunchable().getUnitControllerRegistry().get(dalUnitConfig.getId());
//            if (unitController instanceof ReedContactController) {
//                reedContactControllerList.add((ReedContactController) unitController);
//            }
//        }
//
//        ContactState closedState = ContactState.newBuilder().setValue(ContactState.State.CLOSED).build();
//        for (ReedContactController reedContact : reedContactControllerList) {
//            reedContact.updateContactStateProvider(closedState);
//        }
//
//        System.out.println("ping");
//        connectionRemote.ping().get();
//        System.out.println("ping done");
//        System.out.println("request data of " + ScopeGenerator.generateStringRep(connectionRemote.getScope()));
//        System.out.println("got data: " + connectionRemote.requestData().get().getDoorState().getValue());
//        while (connectionRemote.getDoorState().getValue() != DoorState.State.CLOSED) {
////            System.out.println("current state: " + locationRemote.getData());
//            System.out.println("current temp: " + connectionRemote.getDoorState().getValue() + " waiting for: " + DoorState.State.CLOSED);
//            Thread.sleep(10);
//        }
//        Assert.assertEquals("Doorstate of the connection has not been updated!", DoorState.State.CLOSED, connectionRemote.getDoorState().getValue());
//
//        ContactState openState = ContactState.newBuilder().setValue(ContactState.State.OPEN).build();
//        for (ReedContactController reedContact : reedContactControllerList) {
//            reedContact.updateContactStateProvider(openState);
//        }
//
//        System.out.println("ping");
//        connectionRemote.ping().get();
//        System.out.println("ping done");
//        System.out.println("request data of " + ScopeGenerator.generateStringRep(connectionRemote.getScope()));
//        System.out.println("got data: " + connectionRemote.requestData().get().getDoorState().getValue());
//        while (connectionRemote.getDoorState().getValue() != DoorState.State.OPEN) {
////            System.out.println("current state: " + locationRemote.getData());
//            System.out.println("current temp: " + connectionRemote.getDoorState().getValue() + " waiting for: " + DoorState.State.OPEN);
//            Thread.sleep(10);
//        }
//        Assert.assertEquals("Doorstate of the connection has not been updated!", DoorState.State.OPEN, connectionRemote.getDoorState().getValue());
//    }
}
