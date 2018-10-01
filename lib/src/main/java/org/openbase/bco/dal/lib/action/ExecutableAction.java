package org.openbase.bco.dal.lib.action;

import org.openbase.jul.iface.Executable;
import org.openbase.jul.iface.Initializable;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionDescriptionType.ActionDescription;

public interface ExecutableAction extends Action, Executable<ActionDescription>, Initializable<ActionDescription> {
}
