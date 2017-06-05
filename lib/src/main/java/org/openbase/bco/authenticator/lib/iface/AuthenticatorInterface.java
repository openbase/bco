/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.authenticator.lib.iface;

import com.google.protobuf.ByteString;
import rst.domotic.authentification.AuthenticatorTicketType.AuthenticatorTicket;
import rst.domotic.authentification.AuthenticatorType.Authenticator;
import rst.domotic.authentification.LoginResponseType.LoginResponse;
import rst.domotic.authentification.TicketType.Ticket;

/*-
 * #%L
 * BCO Authentification Library
 * %%
 * Copyright (C) 2017 openbase.org
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

/**
 *
 * @author Tamino Huxohl <thuxohl@techfak.uni-bielefel.de>
 */
public interface AuthenticatorInterface {
 
    public default void test() {
        AuthenticatorTicket.Builder authenticatorTicket = AuthenticatorTicket.newBuilder();
        // encrypted as bytestring authenticatorTicket.setAuthenticator("");
        // encrypted as bytestring authenticatorTicket.setTicket("");
    
        Authenticator.Builder authenticator = Authenticator.newBuilder();
        authenticator.setClientId("");
        //authenticator.setTimestamp(null);
        
        LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
        loginResponse.setSessionKey(ByteString.EMPTY);
        loginResponse.setTicket(ByteString.EMPTY);
        
        Ticket.Builder ticket = Ticket.newBuilder();
        ticket.setClientId("");
        ticket.setClientIp("");
        ticket.setSessionKey("");
        // ticket.setValidityPeriod(value);
    }
}
