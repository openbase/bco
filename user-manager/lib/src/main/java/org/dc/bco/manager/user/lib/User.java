/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.user.lib;

import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.iface.Identifiable;
import rst.authorization.UserActivityType.UserActivity;
import rst.authorization.UserPresenceStateType.UserPresenceState;

/**
 *
 * @author mpohling
 */
public interface User extends Identifiable<String> {

    public String getUserName() throws NotAvailableException;

    public UserActivity getUserActivity() throws NotAvailableException;

    public UserPresenceState getUserPresenceState() throws NotAvailableException;
}
