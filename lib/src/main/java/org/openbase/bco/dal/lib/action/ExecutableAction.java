package org.openbase.bco.dal.lib.action;

import org.openbase.jul.iface.Executable;
import org.openbase.jul.iface.Initializable;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;

public interface ExecutableAction extends Action, Executable<ActionFuture>, Initializable<ActionDescription> {
}
