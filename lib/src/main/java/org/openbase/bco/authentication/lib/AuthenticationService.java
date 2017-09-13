package org.openbase.bco.authentication.lib;

import java.util.concurrent.Future;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.PermissionDeniedException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.iface.annotations.RPCMethod;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
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
     * @throws RejectedException If timestamp in Authenticator does not fit to time period in TGT,
     * if clientID in Authenticator does not match clientID in TGT or, if the decryption of the
     * Authenticator or TGT fails, probably because the wrong keys were used.
     * @throws CouldNotPerformException In the case of an internal server error or if the remote call fails.
     */
    @RPCMethod
    public Future<TicketSessionKeyWrapper> requestClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws RejectedException, CouldNotPerformException;

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
     * @throws RejectedException If timestamp in Authenticator does not fit to time period in TGT,
     * if clientID in Authenticator does not match clientID in TGT or, if the decryption of the
     * Authenticator or CST fails, probably because the wrong keys were used.
     * @throws CouldNotPerformException In the case of an internal server error or if the remote call fails.
     */
    @RPCMethod
    public Future<TicketAuthenticatorWrapper> validateClientServerTicket(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws RejectedException, CouldNotPerformException;

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
     * @throws PermissionDeniedException If the user has no permission to change this password.
     */
    @RPCMethod
    public Future<TicketAuthenticatorWrapper> changeCredentials(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException, RejectedException, PermissionDeniedException;

    /**
     * Registers a client or user.
     *
     * @param loginCredentialsChange Wrapper containing the user's ID, password or public key, isAdmin flag,
     * and a TicketAuthenticatorWrapper to authenticate the user.
     * @return TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     * @throws RejectedException If the password change fails (invalid ticket, user has no permission, old password doesn't match)
     * or if the decryption fails, because the wrong keys were used.
     * @throws PermissionDeniedException If the user has no permission to change this password.
     */
    @RPCMethod
    public Future<TicketAuthenticatorWrapper> register(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException, RejectedException, PermissionDeniedException;

    /**
     * Removes a user or client.
     *
     * @param loginCredentialsChange change of credentials (id of user to remove)
     * @return TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     * @throws RejectedException If the password change fails (invalid ticket, user has no permission, old password doesn't match)
     * or if the decryption fails, because the wrong keys were used.
     * @throws PermissionDeniedException If the user has no permission to change this password.
     */
    @RPCMethod
    public Future<TicketAuthenticatorWrapper> removeUser(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException, RejectedException, PermissionDeniedException;

    /**
     * Appoints a normal user to an administrator.
     *
     * @param loginCredentialsChange Wrapper containing the user's ID, password or public key, isAdmin flag,
     * and a TicketAuthenticatorWrapper to authenticate the user.
     * @return TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     * @throws RejectedException If the password change fails (invalid ticket, user has no permission)
     * or if the decryption fails, because the wrong keys were used.
     * @throws PermissionDeniedException If the user has no permission to change this password.
     */
    @RPCMethod
    public Future<TicketAuthenticatorWrapper> setAdministrator(LoginCredentialsChange loginCredentialsChange) throws CouldNotPerformException, RejectedException, PermissionDeniedException;
    
    /**
     * Validates the client server ticket and returns the service server secret key encrypted
     * with the session key. This method will only work if a special client is logged in.
     * 
     * @param ticketAuthenticatorWrapper a wrapper containing the authenticator
     * encrypted with the session key and the unchanged ClientServerTicket
     * @return an authenticated value containing the updated client server ticket an the encrypted service server secret key
     * @throws CouldNotPerformException if the validation of the client server ticket fails or the logged in client is not the service server
     */
    @RPCMethod
    public Future<AuthenticatedValue> requestServiceServerSecretKey(TicketAuthenticatorWrapper ticketAuthenticatorWrapper) throws CouldNotPerformException;

    /**
     * Returns whether a given user has admin rights or not.
     *
     * @param userId ID of the user to check for.
     * @return True, if the user is admin, false if not.
     * @throws NotAvailableException If the user could not be found.
     * @throws CouldNotPerformException
     */
    @RPCMethod
    public Future<Boolean> isAdmin(String userId) throws NotAvailableException, CouldNotPerformException;
    
    @RPCMethod
    public Future<Boolean> hasUser(String userId) throws CouldNotPerformException;
}
