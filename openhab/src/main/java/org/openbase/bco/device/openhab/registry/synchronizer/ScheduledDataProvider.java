package org.openbase.bco.device.openhab.registry.synchronizer;

/*-
 * #%L
 * BCO Openhab Device Manager
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.pattern.AbstractObservable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledDataProvider implements DataProvider, Activatable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledDataProvider.class);

    private final AbstractObservable observable;
    private final Runnable runnable;
    private final long delay;
    private final long period;
    private final TimeUnit timeUnit;

    private ScheduledFuture scheduledFuture;

    public ScheduledDataProvider(final long delay, final long period, final TimeUnit timeUnit) {
        this.delay = delay;
        this.period = period;
        this.timeUnit = timeUnit;
        this.observable = new ObservableImpl(false);
        this.runnable = () -> {
            try {
                observable.notifyObservers("");
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Scheduled observer notification failed", ex, LOGGER);
            }
        };
    }

    @Override
    public void activate() {
        try {
            scheduledFuture = GlobalScheduledExecutorService.scheduleAtFixedRate(runnable, period, delay, timeUnit);
        } catch (NotAvailableException ex) {
            // the exception is only thrown if the runnable is null which cannot be the case here
            ExceptionPrinter.printHistory(new FatalImplementationErrorException(this, ex), LOGGER);
        }
    }

    @Override
    public void deactivate() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }

    @Override
    public boolean isActive() {
        return scheduledFuture != null;
    }

    @Override
    public boolean isDataAvailable() {
        return true;
    }

    @Override
    public Class getDataClass() {
        return null;
    }

    @Override
    public Object getData() {
        return null;
    }

    @Override
    public CompletableFuture getDataFuture() {
        return null;
    }

    @Override
    public void addDataObserver(Observer observer) {
        this.observable.addObserver(observer);
    }

    @Override
    public void removeDataObserver(Observer observer) {
        this.observable.removeObserver(observer);
    }

    @Override
    public void waitForData() {
    }

    @Override
    public void waitForData(long l, TimeUnit timeUnit) {
    }
}
