package org.openbase.bco.authentication.lib;

import org.openbase.jul.annotation.RPCMethod;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;

import java.util.concurrent.Future;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2020 openbase.org
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
     * session key for the TicketGrantingService encrypted with the credentials
     * of the user and/or client defined in the userClientPair.
     * <p>
     * Afterwards the client has to decrypt the session key with his password
     * and create an authenticator encrypted with it. Then the unchanged
     * TicketGrantingTicket and the encrypted Authenticator form a
     * TicketAuthenticatorWrapper which is used to request a ClientServerTicket.
     *
     * @param userClientPair pair identifying the user and/or client requesting the ticket.
     *
     * @return the described TicketSessionKeyWrapper
     * <p>
     * The initial cause can be detected by calling .get() catching the cancellation exception and resolving the initial cause via ExceptionProcessor.getInitialCause(...).
     * Initial cause could be one of the following:
     * <p>
     * * NotAvailableException    If the clientId could not be found.
     * * CouldNotPerformException In the case of an internal server error or if the remote call fails.
     */
    @RPCMethod
    Future<TicketSessionKeyWrapper> requestTicketGrantingTicket(final UserClientPair userClientPair);

    /**
     * Request a ClientServerTicket from the AuthenticatorService. The reply is
     * a TicketSessionKeyWrapper that contains the ClientServerTicket encrypted
     * with the private key of the ServiceServer and the session key encrypted
     * with the TicketGrantingService session key that the client received when
     * requesting the TicketGrantingTicket.
     * <p>
     * Afterwards the client has to decrypt the session key with the
     * TicketGrantingTicket session key and create an authenticator encrypted
     * with it. Then the unchanged ClientServerTicket and the encrypted
     * Authenticator form a TicketAuthenticatorWrapper which send to validate
     * the client every time he wants to perform an action.
     *
     * @param ticketAuthenticatorWrapper a wrapper containing the authenticator
     *                                   encrypted with the TicketGrantingService session key and the unchanged
     *                                   TicketGrantingTicket
     *
     * @return a wrapper containing a ClientServerTicket and a session key as
     * described above
     * <p>
     * The initial cause can be detected by calling .get() catching the cancellation exception and resolving the initial cause via ExceptionProcessor.getInitialCause(...).
     * Initial cause could be one of the following:
     * <p>
     * * RejectedException        If timestamp in Authenticator does not fit to time period in TGT, if clientID in Authenticator does not match clientID in TGT or, if the decryption of the Authenticator or TGT fails, probably because the wrong keys were used.
     * * CouldNotPerformException In the case of an internal server error or if the remote call fails.
     */
    @RPCMethod
    Future<TicketSessionKeyWrapper> requestClientServerTicket(final TicketAuthenticatorWrapper ticketAuthenticatorWrapper);

    /**
     * Validate a ClientServerTicket. If validation is successful the reply is
     * a TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     *
     * @param ticketAuthenticatorWrapper a wrapper containing the authenticator
     *                                   encrypted with the session key and the unchanged ClientServerTicket
     *
     * @return a TicketAuthenticatorWrapper as described above
     * <p>
     * The initial cause can be detected by calling .get() catching the cancellation exception and resolving the initial cause via ExceptionProcessor.getInitialCause(...).
     * Initial cause could be one of the following:
     * <p>
     * * RejectedException        If timestamp in Authenticator does not fit to time period in TGT, if clientID in Authenticator does not match clientID in TGT or, if the decryption of the Authenticator or CST fails, probably because the wrong keys were used.
     * * CouldNotPerformException In the case of an internal server error or if the remote call fails.
     */
    @RPCMethod
    Future<TicketAuthenticatorWrapper> validateClientServerTicket(final TicketAuthenticatorWrapper ticketAuthenticatorWrapper);

    /**
     * Changes the credentials for a given user. Note that admins are allowed to change the credentials of other users
     * without verification of the old credentials.
     *
     * @param authenticatedValue authenticated value containing a ticket of the current session and a login credentials
     *                           change object for the user whose credentials should be changes as its value encrypted with the session key.
     *                           For normal users the old credentials in the login credentials change object has to match
     *                           its current credentials.
     *
     * @return TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     * <p>
     * The initial cause can be detected by calling .get() catching the cancellation exception and resolving the initial cause via ExceptionProcessor.getInitialCause(...).
     * Initial cause could be one of the following:
     * <p>
     * * RejectedException         If the password change fails (invalid ticket, user has no permission, old password doesn't match).
     * * PermissionDeniedException If the user has no permission to change this password.
     */
    @RPCMethod
    Future<AuthenticatedValue> changeCredentials(final AuthenticatedValue authenticatedValue);

    /**
     * Registers a client or user. Note that only admins may register an admin.
     *
     * @param authenticatedValue authenticated value containing a ticket of the current session and a login credentials
     *                           object for the user to be registered as its value encrypted with the session key.
     *
     * @return TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     * <p>
     * The initial cause can be detected by calling .get() catching the cancellation exception and resolving the initial cause via ExceptionProcessor.getInitialCause(...).
     * Initial cause could be one of the following:
     * <p>
     * * RejectedException         If the password change fails (invalid ticket, user has no permission, old password doesn't match) or if the decryption fails, because the wrong keys were used.
     * * PermissionDeniedException If the user has no permission to change this password.
     */
    @RPCMethod
    Future<AuthenticatedValue> register(final AuthenticatedValue authenticatedValue);

    /**
     * Remove a user or client. Note:
     * <ul>
     * <li>normal user/clients only may remove themselves</li>
     * <li>admins may remove other users or clients</li>
     * <li>the last admin cannot remove itself so that at least one admin always remains</li>
     * </ul>
     *
     * @param authenticatedValue authenticated value containing a ticket of the current session and the id of the user
     *                           to be removed as its value encrypted with the session key.
     *
     * @return TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     * <p>
     * The initial cause can be detected by calling .get() catching the cancellation exception and resolving the initial cause via ExceptionProcessor.getInitialCause(...).
     * Initial cause could be one of the following:
     * <p>
     * * RejectedException         If the password change fails (invalid ticket, user has no permission, old password doesn't match) or if the decryption fails, because the wrong keys were used.
     * * PermissionDeniedException If the user has no permission to change this password.
     */
    @RPCMethod
    Future<AuthenticatedValue> removeUser(final AuthenticatedValue authenticatedValue);

    /**
     * Change the admin status of a user. This may only be done by administrators. Note that an admin cannot demote
     * itself to a normal user so that at least one admin remains. An admin can only be changed to a normal user by
     * another admin.
     *
     * @param authenticatedValue authenticated value containing a ticket of the current session and the id of the user
     *                           whose admin settings are changed as its value encrypted with the session key.
     *
     * @return TicketAuthenticatorWrapper which contains an updated validity period in
     * the ClientServerTicket and an updated timestamp in the authenticator
     * which has to be verified by the client to make sure that its the correct
     * server answering the request.
     * <p>
     * The initial cause can be detected by calling .get() catching the cancellation exception and resolving the initial cause via ExceptionProcessor.getInitialCause(...).
     * Initial cause could be one of the following:
     * * RejectedException If the password change fails (invalid ticket, user has no permission) or if the decryption fails, because the wrong keys were used.
     * * PermissionDeniedException If the user has no permission to change this password.
     */
    @RPCMethod
    Future<AuthenticatedValue> setAdministrator(final AuthenticatedValue authenticatedValue);

    /**
     * Validates the client server ticket and returns the service server secret key encrypted
     * with the session key. This method will only work if a special client is logged in.
     *
     * @param ticketAuthenticatorWrapper a wrapper containing the authenticator
     *                                   encrypted with the session key and the unchanged ClientServerTicket
     *
     * @return an authenticated value containing the updated client server ticket an the encrypted service server secret key
     * <p>
     * The initial cause can be detected by calling .get() catching the cancellation exception and resolving the initial cause via ExceptionProcessor.getInitialCause(...).
     * Initial cause could be one of the following:
     * <p>
     * * CouldNotPerformException if the validation of the client server ticket fails or the logged in client is not the service server
     */
    @RPCMethod
    Future<AuthenticatedValue> requestServiceServerSecretKey(final TicketAuthenticatorWrapper ticketAuthenticatorWrapper);

    /**
     * Returns whether a given user has admin rights or not.
     *
     * @param userId ID of the user to check for.
     *
     * @return True, if the user is admin, false if not.
     * <p>
     * The initial cause can be detected by calling .get() catching the cancellation exception and resolving the initial cause via ExceptionProcessor.getInitialCause(...).
     * Initial cause could be one of the following:
     * * NotAvailableException    If the user could not be found.
     * * CouldNotPerformException if the test could no be performed
     */
    @RPCMethod
    Future<Boolean> isAdmin(final String userId);

    /**
     * Query if the authenticator has credentials for a given user/client.
     *
     * @param userOrClientId the id of the user or client checked.
     *
     * @return true if the user has credentials at the authenticator, else false.
     */
    @RPCMethod
    Future<Boolean> hasUser(final String userOrClientId);
}
