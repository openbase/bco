package org.openbase.bco.authentication.lib;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import rst.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import rst.domotic.authentication.LoginCredentialsChangeType.LoginCredentialsChange;

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
/**
 * Interface defining a service for Kerberos authentication.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public interface AuthenticationService {

    /**
     * Request a TicketGrantingTicket from the AuthenticatorService. The reply
     * is a TicketSessionKeyWrapper that contains the TicketGrantingTicket
     * encrypted with the private key of the TicketGrantingService and the
     * session key for the TicketGrantingService encrypted with the client
     * password.
     *
     * Afterwards the client has to decrypt the session key with his password
     * and create an authenticator encrypted with it. Then the unchanged
     * TicketGrantingTicket and the encrypted Authenticator form a
     * TicketAuthenticatorWrapper which is used to request a ClientServerTicket.
     *
     * @param clientId the id of the client whose password is used for the
     * encryption of the session key
     * @return the described TicketSessionKeyWrapper
     * @throws NotAvailableException If the clientId could not be found.
     * @throws CouldNotPerformException In the case of an internal server error or if the remote call fails.
     */
    @RPCMethod
    public Future<TicketSessionKeyWrapper> requestTicketGrantingTicket(String clientId) throws NotAvailableException, CouldNotPerformException;

    /**
     * Request a ClientServerTicket from the AuthenticatorService. The reply is
     * a TicketSessionKeyWrapper that contains the ClientServerTicket encrypted
     * with the private key of the ServiceServer and the session key encrypted
     * with the TicketGrantingService session key that the client received when
     * requesting the TicketGrantingTicket.
     *
     * Afterwards the client has to decrypt the session key with the
     * TicketGrantingTicket session key and create an authenticator encrypted
     * with it. Then the unchanged ClientServerTicket and the encrypted
     * Authenticator form a TicketAuthenticatorWrapper which send to validate
     * the client every time he wants to perform an action.
     *
     * @param ticketAuthenticatorWrapper a wrapper containing the authenticator
     * encrypted with the TicketGrantingService session key and the unchanged
     * TicketGrantingTicket
     * @return a wrapper containing a ClientServerTicket and a session key as
     * described above
     * @throws RejectedException If timestamp in Authenticator does not fit to time period in TGT
     * or, if clientID in Authenticator does not match clientID in TGT
     * @throws StreamCorruptedException If the decryption of the Authenticator or TGT fails, probably because the wrong keys were used.
     * @throws CouldNotPerformException In the case of an internal server error or if the remote call fails.
     */
    @RPCMethod
    public Future<TicketSessionKeyWrapper> requestClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws RejectedException, StreamCorruptedException, CouldNotPerformException;

    /**
     * Validate a ClientServierTicket. If validation is successful the reply is
     * a TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     *
     * @param ticketAuthenticatorWrapper a wrapper containing the authenticator
     * encrypted with the session key and the unchanged ClientServerTicket
     * @return a TicketAuthenticatorWrapper as described above
     * @throws RejectedException If timestamp in Authenticator does not fit to time period in TGT
     * or, if clientID in Authenticator does not match clientID in TGT
     * @throws StreamCorruptedException If the decryption of the Authenticator or CST fails, probably because the wrong keys were used.
     * @throws CouldNotPerformException In the case of an internal server error or if the remote call fails.
     */
    @RPCMethod
    public Future<TicketAuthenticatorWrapper> validateClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws RejectedException, StreamCorruptedException, CouldNotPerformException;

    /**
     * Changes the credentials for a given user.
     *
     * @param loginCredentialsChange Wrapper containing the user's ID, new and old password,
     * and a TicketAuthenticatorWrapper to authenticate the user.
     * @return TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     * @throws RejectedException If the password change fails (invalid ticket, user has no permission, old password doesn't match).
     * @throws StreamCorruptedException If any decryption fails.
     * @throws IOException If de- or encryption fail because of a general I/O error.
     * @throws PermissionDeniedException If the user has no permission to change this password.
     */
    @RPCMethod
    public Future<TicketAuthenticatorWrapper> changeCredentials(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException, RejectedException, StreamCorruptedException, IOException, PermissionDeniedException;

    /**
     * Register a new client in the authentication registry. This is only allowed if the authenticator is in registration mode.
     * The LoginCredentialsChange type contains more fields than needed because its also used to update an existing user.
     * To use it for registration only the id field containing the client id and the new password field which contains the passwords are needed.
     *
     * @param loginCredentialsChange the login credentials containing the information as described above
     * @return a future of the action of registering the client
     * @throws CouldNotPerformException if the authenticator is not in registration mode
     */
    @RPCMethod
    public Future<Void> registerClient(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException;
    
    /**
     * 
     * @return
     * @throws CouldNotPerformException 
     */
    @RPCMethod
    public Future<Boolean> isInRegistrationMode() throws CouldNotPerformException;
}
