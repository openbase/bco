package org.openbase.bco.app.influxdbconnector;

/*-
 * #%L
 * BCO InfluxDB Connector
 * %%
 * Copyright (C) 2018 - 2019 openbase.org
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

import org.openbase.bco.dal.control.layer.unit.app.AbstractAppController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class InfluxDbconnectorApp extends AbstractAppController  {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Future task;

    public InfluxDbconnectorApp() throws InstantiationException {

    }

    @Override
    protected ActionDescription execute(ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        logger.info("execute influx db connector");
        task = GlobalCachedExecutorService.submit(() -> {
            while (!task.isCancelled()) {
                try {
                    logger.info("connect to db at: "+ generateVariablePool().getValue("INFLUXDB_URL" ));
                    Thread.sleep(5000);
                } catch (InterruptedException | CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });
        return activationState.getResponsibleAction();
    }

    @Override
    protected void stop(ActivationState activationState) throws CouldNotPerformException, InterruptedException {
        if(task != null && !task.isDone()) {
            task.cancel(false);
        }
    }
}
