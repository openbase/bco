package org.openbase.bco.app.influxdbconnector;

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
