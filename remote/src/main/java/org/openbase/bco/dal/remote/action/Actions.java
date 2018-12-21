package org.openbase.bco.dal.remote.action;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;

import java.util.concurrent.Future;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class Actions {

    public static void waitForExecution(final Future<ActionDescription> actionFuture) throws CouldNotPerformException, InterruptedException {
        new RemoteAction(actionFuture).waitForExecution();
    }
}
