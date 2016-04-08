package org.dc.bco.manager.scene.binding.openhab;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
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
import org.dc.bco.manager.scene.binding.openhab.comm.OpenHABCommunicator;
import org.dc.bco.manager.scene.binding.openhab.comm.OpenHABCommunicatorImpl;
import org.dc.bco.manager.scene.core.SceneManagerController;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class OpenHABBindingImpl implements OpenHABBinding {

    private static final Logger logger = LoggerFactory.getLogger(OpenHABBindingImpl.class);

    private static OpenHABBinding instance;
//    private SceneManagerController sceneManagerController;
    private OpenHABCommunicatorImpl busCommunicator;

    public OpenHABBindingImpl() throws InstantiationException {
        try {
            instance = this;
            this.busCommunicator = new OpenHABCommunicatorImpl();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public static OpenHABBinding getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(OpenHABBinding.class);
        }
        return instance;
    }

    public void init() throws InitializationException, InterruptedException {
        try {
//            this.sceneManagerController = new SceneManagerController();
            this.busCommunicator.init();
//            this.sceneManagerController.init();
            this.busCommunicator.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() throws InterruptedException {
//        if (sceneManagerController != null) {
//            sceneManagerController.shutdown();
//        }

        if (busCommunicator != null) {
            busCommunicator.shutdown();
        }
        instance = null;
    }

    @Override
    public OpenHABCommunicator getBusCommunicator() throws NotAvailableException {
        return busCommunicator;
    }
}
