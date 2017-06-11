package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
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

import java.util.List;
import org.openbase.jul.exception.RejectedException;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;

/**
 *
 * @author sebastian
 */
public interface AuthenticationClientHandler {
    
    /**
     * Handles a KDC response
     * Decrypts the TGS session key with client's hashed password
     * Creates an Authenticator containing the clientID and current timestamp encrypted with the TGS session key
     * @param clientID Identifier of the client - must be present in client database
     * @param hashedClientPassword Client's hashed password
     * @param wrapper TicketSessionKeyWrapper containing the TGT and TGS session key
     * @return Returns a list of objects containing: 
     *         1. An TicketAuthenticatorWrapperWrapper containing both the TGT and Authenticator
     *         2. A SessionKey representing the TGS session key
     * @TODO: Exception description
     */
    public List<Object> handleKDCResponse(String clientID, byte[] hashedClientPassword, TicketSessionKeyWrapper wrapper) throws RejectedException; 

    /**
     * Handles a TGS response
     * Decrypts the SS session key with TGS session key
     * Creates an Authenticator containing the clientID and empty timestamp encrypted with the SS session key
     * @param clientID Identifier of the client - must be present in client database
     * @param TGSSessionKey TGS session key provided by handleKDCResponse()
     * @param wrapper TicketSessionKeyWrapper containing the CST and SS session key
     * @return Returns a list of objects containing: 
     *         1. An TicketAuthenticatorWrapperWrapper containing both the CST and Authenticator
     *         2. A SessionKey representing the SS session key
     * @TODO: Exception description
     */
    public List<Object> handleTGSResponse(String clientID, byte[] TGSSessionKey, TicketSessionKeyWrapper wrapper) throws RejectedException;
    
    /**
     * Initializes a SS request
     * Sets current timestamp in Authenticator
     * @param SSSessionKey SS session key provided by handleTGSResponse()
     * @param wrapper TicketAuthenticatorWrapper wrapper that contains both encrypted Authenticator and CST
     * @return Returns a wrapper class containing both the CST and modified Authenticator
     * @TODO: Exception description
     */
    public TicketAuthenticatorWrapper initSSRequest(byte[] SSSessionKey, TicketAuthenticatorWrapper wrapper) throws RejectedException;
    
    /**
     * Handles a SS response
     * Decrypts Authenticator of both last- and currentWrapper with SSSessionKey
     * Compares timestamps of both Authenticators with each other
     * @param SSSessionKey SS session key provided by handleTGSResponse()
     * @param lastWrapper Last TicketAuthenticatorWrapper provided by either handleTGSResponse() or handleSSResponse()
     * @param currentWrapper Current TicketAuthenticatorWrapper provided by (Remote?)
     * @return Returns an TicketAuthenticatorWrapperWrapper containing both the CST and Authenticator
     * @throws RejectedException Throws, if timestamps do not match
     * @TODO: Exception description
     */
    public TicketAuthenticatorWrapper handleSSResponse(byte[] SSSessionKey, TicketAuthenticatorWrapper lastWrapper, TicketAuthenticatorWrapper currentWrapper) throws RejectedException;
}
