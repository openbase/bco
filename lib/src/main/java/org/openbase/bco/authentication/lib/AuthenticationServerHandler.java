package org.openbase.bco.authentication.lib;

/*-
 * #%L
 * BCO Authentication Library
 * %%
 * Copyright (C) 2017 - 2019 openbase.org
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

import com.google.protobuf.ByteString;
import org.openbase.bco.authentication.lib.exception.SessionExpiredException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.authentication.AuthenticatorType.Authenticator;
import org.openbase.type.domotic.authentication.AuthenticatorType.AuthenticatorOrBuilder;
import org.openbase.type.domotic.authentication.LoginCredentialsType.LoginCredentials;
import org.openbase.type.domotic.authentication.TicketAuthenticatorWrapperType.TicketAuthenticatorWrapper;
import org.openbase.type.domotic.authentication.TicketSessionKeyWrapperType.TicketSessionKeyWrapper;
import org.openbase.type.domotic.authentication.TicketType.Ticket;
import org.openbase.type.domotic.authentication.TicketType.TicketOrBuilder;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.openbase.type.timing.IntervalType.Interval;
import org.openbase.type.timing.TimestampType.Timestamp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sfast@techfak.uni-bielefeld.de">Sebastian Fast</a>
 */
public class AuthenticationServerHandler {

    public static final long MAX_TIME_DIFF_SERVER_CLIENT = TimeUnit.MINUTES.toMillis(2);

    /**
     * Handles a Key Distribution Center (KDC) login request
     * Creates a Ticket Granting Server (TGS) session key that is encrypted by the client's password
     * Creates a Ticket Granting Ticket (TGT) that is encrypted by TGS private key
     *
     * @param userClientPair                 pair identifying the client and user which are logged in
     * @param userCredentials                the credentials of the user which are only needed if a user is defined in
     *                                       the userClientPair
     * @param clientCredentials              the credentials of the client which are only needed if a client is defined in
     *                                       the userClientPair
     * @param ticketGrantingServiceSecretKey TGS secret key generated by controller or saved somewhere in the system
     * @param validityTime                   the time in milliseconds from now how long the TGT is valid
     *
     * @return Returns wrapper class containing both the TGT and TGS session key
     * <p>
     * <<<<<<< HEAD
     * =======
     *
     * @throws NotAvailableException    Throws, if clientID was not found in database
     *                                  >>>>>>> master
     * @throws CouldNotPerformException If the data for the remotes has not been synchronized yet.
     */
    public static TicketSessionKeyWrapper handleKDCRequest(final UserClientPair userClientPair, final LoginCredentials userCredentials, final LoginCredentials clientCredentials, final byte[] ticketGrantingServiceSecretKey, final long validityTime)
            throws CouldNotPerformException {
        byte[] ticketGrantingServiceSessionKey = EncryptionHelper.generateKey();

        // create ticket granting ticket
        final ByteString ticketGrantingTicket = updateAndEncryptTicket(Ticket.newBuilder().setUserClientPair(userClientPair), validityTime, ticketGrantingServiceSessionKey, ticketGrantingServiceSecretKey);

        // create TicketSessionKeyWrapper
        TicketSessionKeyWrapper.Builder ticketSessionKeyWrapper = TicketSessionKeyWrapper.newBuilder();
        ticketSessionKeyWrapper.setTicket(ticketGrantingTicket);

        if (userClientPair.hasUserId() && !userClientPair.getUserId().isEmpty()) {
            if (userCredentials == null) {
                throw new NotAvailableException("user credentials");
            }
            ticketGrantingServiceSessionKey = EncryptionHelper.encrypt(ticketGrantingServiceSessionKey, userCredentials);
        }
        if (userClientPair.hasClientId() && !userClientPair.getClientId().isEmpty()) {
            if (clientCredentials == null) {
                throw new NotAvailableException("client credentials");
            }
            ticketGrantingServiceSessionKey = EncryptionHelper.encrypt(ticketGrantingServiceSessionKey, clientCredentials);
        }

        ticketSessionKeyWrapper.setSessionKey(ByteString.copyFrom(ticketGrantingServiceSessionKey));

        return ticketSessionKeyWrapper.build();
    }

    /**
     * Handles a Ticket Granting Service (TGS) request
     * Creates a Service Server (SS) session key that is encrypted with the TGS session key
     * Creates a Client Server Ticket (CST) that is encrypted by SS private key
     *
     * @param ticketGrantingServiceSecretKey TGS secret key generated by controller or saved somewhere in the system
     * @param serviceServerSecretKey         TGS secret key generated by controller or saved somewhere in the system
     * @param wrapper                        TicketAuthenticatorWrapperWrapper that contains both encrypted Authenticator and TGT
     * @param validityTime                   time in milli seconds how long the new ticket is valid from now on
     *
     * @return Returns a wrapper class containing both the CST and SS session key
     *
     * @throws RejectedException        If timestamp in Authenticator does not fit to time period in TGT
     *                                  or, if clientID in Authenticator does not match clientID in TGT
     * @throws CouldNotPerformException If de- or encryption fail.
     */
    public static TicketSessionKeyWrapper handleTGSRequest(final byte[] ticketGrantingServiceSecretKey, final byte[] serviceServerSecretKey, final TicketAuthenticatorWrapper wrapper, final long validityTime) throws RejectedException, CouldNotPerformException {
        // decrypt ticket and authenticator
        Ticket ticketGrantingTicket = EncryptionHelper.decryptSymmetric(wrapper.getTicket(), ticketGrantingServiceSecretKey, Ticket.class);
        byte[] ticketGrantingServiceSessionKey = ticketGrantingTicket.getSessionKeyBytes().toByteArray();
        Authenticator authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), ticketGrantingServiceSessionKey, Authenticator.class);

        // compare clientIDs and timestamp to period
        AuthenticationServerHandler.validateTicket(ticketGrantingTicket, authenticator);

        // generate new session key
        byte[] serviceServerSessionKey = EncryptionHelper.generateKey();

        // update validity interval, session key and encrypt
        final ByteString clientServerTicket = EncryptionHelper.encryptSymmetric(serviceServerSessionKey, ticketGrantingServiceSessionKey);

        // create TicketSessionKeyWrapper
        TicketSessionKeyWrapper.Builder ticketSessionKeyWrapper = TicketSessionKeyWrapper.newBuilder();
        ticketSessionKeyWrapper.setTicket(updateAndEncryptTicket(ticketGrantingTicket.toBuilder(), validityTime, serviceServerSessionKey, serviceServerSecretKey));
        ticketSessionKeyWrapper.setSessionKey(clientServerTicket);

        return ticketSessionKeyWrapper.build();
    }

    /**
     * Update validity period and session key in a ticket. Then encrypt it with the secret key.
     *
     * @param ticket       the ticket to be updated.
     * @param validityTime time in milliseconds how long the ticket should be valid
     * @param sessionKey   the session key added to the ticket.
     * @param secretKey    the key used to encrypt the ticket.
     *
     * @return an updated and encrypted ticket.
     *
     * @throws CouldNotPerformException if encryption fails.
     */
    private static ByteString updateAndEncryptTicket(final Ticket.Builder ticket, final long validityTime, final byte[] sessionKey, final byte[] secretKey) throws CouldNotPerformException {
        ticket.setValidityPeriod(getValidityInterval(validityTime));
        ticket.setSessionKeyBytes(ByteString.copyFrom(sessionKey));
        return EncryptionHelper.encryptSymmetric(ticket.build(), secretKey);
    }

    /**
     * Handles a service method (Remote) request to Service Server (SS) (Manager).
     * Updates given CST's validity period and encrypt again by SS private key.
     * Adds 1 to the authenticator's timestamp to ensure the client that this server responded.
     *
     * @param serviceServerSecretKey SS secret key only known to SS
     * @param wrapper                TicketAuthenticatorWrapper wrapper that contains both encrypted Authenticator and TGT
     * @param validityTime           time in milli seconds how long the new ticket is valid from now on
     *
     * @return Returns a wrapper class containing both the modified CST and unchanged Authenticator
     *
     * @throws RejectedException        If timestamp in Authenticator does not fit to time period in TGT
     *                                  or, if clientID in Authenticator does not match clientID in TGT
     * @throws CouldNotPerformException If de- or encryption fail.
     */
    public static AuthenticationBaseData handleSSRequest(final byte[] serviceServerSecretKey, final TicketAuthenticatorWrapper wrapper, final long validityTime) throws CouldNotPerformException {
        // decrypt ticket and authenticator
        final Ticket.Builder clientServerTicket = EncryptionHelper.decryptSymmetric(wrapper.getTicket(), serviceServerSecretKey, Ticket.class).toBuilder();
        final Authenticator.Builder authenticator = EncryptionHelper.decryptSymmetric(wrapper.getAuthenticator(), clientServerTicket.getSessionKeyBytes().toByteArray(), Authenticator.class).toBuilder();

        // compare clientIDs and timestamp to period
        AuthenticationServerHandler.validateTicket(clientServerTicket, authenticator);

        // update period and session key
        clientServerTicket.setValidityPeriod(getValidityInterval(validityTime));

        // add 1 to authenticator's timestamp
        authenticator.setTimestamp(authenticator.getTimestamp().toBuilder().setTime(authenticator.getTimestamp().getTime() + 1));

        // update TicketAuthenticatorWrapper
        TicketAuthenticatorWrapper.Builder ticketAuthenticatorWrapper = wrapper.toBuilder();
        ticketAuthenticatorWrapper.setTicket(EncryptionHelper.encryptSymmetric(clientServerTicket.build(), serviceServerSecretKey));
        ticketAuthenticatorWrapper.setAuthenticator(EncryptionHelper.encryptSymmetric(authenticator.build(), clientServerTicket.getSessionKeyBytes().toByteArray()));

        return new AuthenticationBaseData(authenticator.getUserClientPair(), clientServerTicket.getSessionKeyBytes().toByteArray(), ticketAuthenticatorWrapper.build());
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("d.M.Y - H:m:s:S");

    public static void validateTicket(final TicketOrBuilder ticket, final AuthenticatorOrBuilder authenticator) throws RejectedException {
        // validate that client and ids in authenticator and ticket match
        if (!ticket.hasUserClientPair() || (ticket.getUserClientPair().getClientId().isEmpty() && ticket.getUserClientPair().getUserId().isEmpty())) {
            throw new RejectedException("Ticket does not contain a valid user client pair");
        }
        if (!authenticator.hasUserClientPair() || (authenticator.getUserClientPair().getClientId().isEmpty() && authenticator.getUserClientPair().getUserId().isEmpty())) {
            throw new RejectedException("Authenticator does not contain a valid user client pair");
        }
        if (!authenticator.getUserClientPair().equals(ticket.getUserClientPair())) {
            System.err.println("Received an erroneous request. Expected[" + ticket.getUserClientPair() + "] but was[" + authenticator.getUserClientPair() + "]");
            throw new RejectedException("UserClientIdPairs do not match");
        }

        // validate that the timestamp from the client request is inside the validation interval of the ticket
        if (!AuthenticationServerHandler.isTimestampInInterval(authenticator.getTimestamp(), ticket.getValidityPeriod())) {
            throw new SessionExpiredException("Request timestamp [" + DATE_FORMAT.format(new Date(TimestampJavaTimeTransform.transform(authenticator.getTimestamp()))) + "]" +
                    " is not within ticket validity period [" + DATE_FORMAT.format(new Date(TimestampJavaTimeTransform.transform(ticket.getValidityPeriod().getBegin()))) + "]" +
                    " - [" + DATE_FORMAT.format(new Date(TimestampJavaTimeTransform.transform(ticket.getValidityPeriod().getEnd()))) + "]");
        }

        // validate that the timestamp does not differ to much from the time of the server
        final Timestamp currentTime = TimestampProcessor.getCurrentTimestamp();
        if (authenticator.getTimestamp().getTime() < (currentTime.getTime() - TimeUnit.MILLISECONDS.toMicros(MAX_TIME_DIFF_SERVER_CLIENT)) ||
                authenticator.getTimestamp().getTime() > (currentTime.getTime() + TimeUnit.MILLISECONDS.toMicros(MAX_TIME_DIFF_SERVER_CLIENT))) {
            throw new SessionExpiredException("Request timestamp [" + DATE_FORMAT.format(new Date(TimestampJavaTimeTransform.transform(authenticator.getTimestamp()))) + "]" +
                    "differs more than 2 minutes from server time [" + DATE_FORMAT.format(new Date(TimestampJavaTimeTransform.transform(currentTime))) + "]");
        }
    }

    /**
     * Test if the timestamp lies in the interval
     *
     * @param timestamp the timestamp checked
     * @param interval  the interval checked
     *
     * @return true if the timestamp is greater equals the start and lower equals the end of the interval
     */
    public static boolean isTimestampInInterval(final Timestamp timestamp, final Interval interval) {
        return timestamp.getTime() >= interval.getBegin().getTime() && timestamp.getTime() <= interval.getEnd().getTime();
    }

    /**
     * Generate an interval which begins now minus acceptable time drift and has an end times of 15 minutes plus acceptable time drift from now.
     *
     * @param validityTime the time in milli seconds how long the interval should go from now
     *
     * @return the above described interval
     */
    public static Interval getValidityInterval(final long validityTime) {
        long currentTime = System.currentTimeMillis();
        Interval.Builder validityInterval = Interval.newBuilder();
        validityInterval.setBegin(TimestampJavaTimeTransform.transform(currentTime - MAX_TIME_DIFF_SERVER_CLIENT));
        validityInterval.setEnd(TimestampJavaTimeTransform.transform(currentTime + validityTime + MAX_TIME_DIFF_SERVER_CLIENT));
        return validityInterval.build();
    }
}
