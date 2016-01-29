/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.user.lib;

import org.dc.jul.exception.InitializationException;
import org.dc.jul.iface.Activatable;
import org.dc.jul.iface.Configurable;
import org.dc.jul.iface.Identifiable;
import rst.authorization.UserConfigType.UserConfig;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public interface UserController extends Identifiable<String>, Configurable<String, UserConfig>, Activatable, User {

    public void init(final UserConfig config) throws InitializationException, InterruptedException;

}
