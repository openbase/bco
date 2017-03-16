package org.openbase.bco.dal.task;

/*-
 * #%L
 * BCO DAL Task
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class BCOTaskServerController implements BCOTaskServer, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BCOTaskServerController.class);

    private static BCOTaskServerController instance;
    private final BCOTaskServer bcoTaskServer;

    public BCOTaskServerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.instance = this;
            this.bcoTaskServer = new BCOTaskServerImpl();
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    public static BCOTaskServerController getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(BCOTaskServerController.class);
        }
        return instance;
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
//        try {
//            
//        } catch (CouldNotPerformException ex) {
//            throw new InitializationException(this, ex);
//        }
    }

    @Override
    public synchronized void activate() throws CouldNotPerformException, InterruptedException {
        try {
            bcoTaskServer.activate();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate " + this, ex);
        }
    }

    @Override
    public boolean isActive() {
        return bcoTaskServer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        try {
            bcoTaskServer.deactivate();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not deative " + this, ex);
        }
    }

    @Override
    public void shutdown() {
        try {
            deactivate();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not shutdown " + bcoTaskServer, ex, LOGGER);
        } finally {
            instance = null;
        }
    }
}
