/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.authenticator.lib.iface;

import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.authentification.AuthenticatorTicketType.AuthenticatorTicket;
import rst.domotic.authentification.LoginResponseType.LoginResponse;

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
 
    @RPCMethod
    public Future<LoginResponse> requestTGT(String clientId) throws CouldNotPerformException;
    
    @RPCMethod
    public Future<LoginResponse> requestCST(AuthenticatorTicket authenticatorTicket) throws CouldNotPerformException;
    
    @RPCMethod
    public Future<AuthenticatorTicket> validateCST(AuthenticatorTicket authenticatorTicket) throws CouldNotPerformException;
}
