package org.openbase.bco.dal.remote.layer.unit.util;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.TimeoutException;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 * @param <D>
 * @param <UR>
 */
public class UnitStateAwaiter<D extends Message, UR extends UnitRemote<D>> {

    protected final Logger logger = LoggerFactory.getLogger(UnitStateAwaiter.class);

    private final SyncObject stateMonitor = new SyncObject("StateMonitor");
    private final Observer<DataProvider<D>, D> dataObserver;
    private final UR unitRemote;

    public UnitStateAwaiter(final UR unitRemote) {
        this.unitRemote = unitRemote;
        this.dataObserver = (DataProvider<D> source, D data) -> {
            synchronized (stateMonitor) {
                stateMonitor.notifyAll();
            }
        };
        this.unitRemote.addDataObserver(dataObserver);
    }

    public void waitForState(StateComparator<D> stateComparator) throws InterruptedException {
        try {
            waitForState(stateComparator, 0);
        } catch (TimeoutException ex) {
            assert false;
        }
    }

    public void waitForState(StateComparator<D> stateComparator, long timeout) throws InterruptedException, TimeoutException {
        synchronized (stateMonitor) {
            long timeWaited = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (stateComparator.equalState(unitRemote.getData())) {
                        return;
                    }
                    logger.info("State not yet reached. Waiting...");
                } catch (NotAvailableException ex) {
                    logger.info("Waiting because unit data not available!");
                }

                // wait till timeout
                long currentTime = System.currentTimeMillis();
                stateMonitor.wait(timeout);
                timeWaited += System.currentTimeMillis() - currentTime;
                logger.info("Woke up! Time waited " + timeWaited + "ms");
                if (timeout != 0 && timeWaited > timeout) {
                    throw new TimeoutException("Timeout expired!");
                }
            }
        }
    }
}
