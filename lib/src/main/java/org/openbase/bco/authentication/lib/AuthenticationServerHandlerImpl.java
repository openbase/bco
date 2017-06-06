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

import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import rst.domotic.authentification.AuthenticatorTicketType;
import rst.domotic.authentification.AuthenticatorType;
import rst.domotic.authentification.LoginResponseType;
import rst.domotic.authentification.TicketType;
import rst.timing.IntervalType;
import rst.timing.TimestampType;

/**
 *
 * @author Sebastian Fast <sfast@techfak.uni-bielefeld.de>
 */
public class AuthenticationServerHandlerImpl implements AuthenticationServerHandler {
    
    @Override
    public LoginResponseType.LoginResponse handleKDCRequest(String clientID, String clientNetworkAddress, byte[] TGSSessionKey, byte[] TGSPrivateKey) throws NotAvailableException, RejectedException {
        // find client's password in database
        String clientPassword = "password";

        // hash password
        byte[] clientPasswordHash = EncryptionHelper.hash(clientPassword);

        // set period
        long start = System.currentTimeMillis();
        long end = start + (5 * 60 * 1000);
        IntervalType.Interval.Builder ib = IntervalType.Interval.newBuilder();
        TimestampType.Timestamp.Builder tb = TimestampType.Timestamp.newBuilder();
        tb.setTime(start);
        ib.setBegin(tb.build());
        tb.setTime(end);
        ib.setEnd(tb.build());

        // create tgt
        TicketType.Ticket.Builder tgtb = TicketType.Ticket.newBuilder();
        tgtb.setClientId(clientID);
        tgtb.setClientIp(clientNetworkAddress);
        tgtb.setValidityPeriod(ib.build());
        tgtb.setSessionKey(TGSSessionKey.toString());

        // create TicketSessionKeyWrapper
        LoginResponseType.LoginResponse.Builder wb = LoginResponseType.LoginResponse.newBuilder();
        wb.setTicket(EncryptionHelper.encrypt(tgtb.build(), TGSPrivateKey));
        wb.setSessionKey(EncryptionHelper.encrypt(TGSSessionKey, clientPasswordHash));

        return wb.build();
    }

    @Override
    public LoginResponseType.LoginResponse handleTGSRequest(byte[] TGSSessionKey, byte[] TGSPrivateKey, byte[] SSSessionKey, byte[] SSPrivateKey, AuthenticatorTicketType.AuthenticatorTicket wrapper) throws RejectedException {
        // decrypt ticket and authenticator
        TicketType.Ticket CST = (TicketType.Ticket) EncryptionHelper.decrypt(wrapper.getTicket(), TGSPrivateKey);
        AuthenticatorType.Authenticator authenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(wrapper.getAuthenticator(), TGSSessionKey);

        // compare clientIDs and timestamp to period
        this.validateTicket(CST, authenticator);

        // set period
        long start = System.currentTimeMillis();
        long end = start + (5 * 60 * 1000);
        IntervalType.Interval.Builder ib = IntervalType.Interval.newBuilder();
        TimestampType.Timestamp.Builder tb = TimestampType.Timestamp.newBuilder();
        tb.setTime(start);
        ib.setBegin(tb.build());
        tb.setTime(end);
        ib.setEnd(tb.build());

        // update period and session key
        TicketType.Ticket.Builder cstb = CST.toBuilder();
        cstb.setValidityPeriod(ib.build());
        cstb.setSessionKey(SSSessionKey.toString());

        // create TicketSessionKeyWrapper
        LoginResponseType.LoginResponse.Builder wb = LoginResponseType.LoginResponse.newBuilder();
        wb.setTicket(EncryptionHelper.encrypt(cstb.build(), SSPrivateKey));
        wb.setSessionKey(EncryptionHelper.encrypt(SSSessionKey, TGSSessionKey));

        return wb.build();
    }

    @Override
    public AuthenticatorTicketType.AuthenticatorTicket handleSSRequest(byte[] SSSessionKey, byte[] SSPrivateKey, AuthenticatorTicketType.AuthenticatorTicket wrapper) throws RejectedException {
        // decrypt ticket and authenticator
        TicketType.Ticket CST = (TicketType.Ticket) EncryptionHelper.decrypt(wrapper.getTicket(), SSPrivateKey);
        AuthenticatorType.Authenticator authenticator = (AuthenticatorType.Authenticator) EncryptionHelper.decrypt(wrapper.getAuthenticator(), SSSessionKey);

        // compare clientIDs and timestamp to period
        this.validateTicket(CST, authenticator);

        // set period
        long start = System.currentTimeMillis();
        long end = start + (5 * 60 * 1000);
        IntervalType.Interval.Builder ib = IntervalType.Interval.newBuilder();
        TimestampType.Timestamp.Builder tb = TimestampType.Timestamp.newBuilder();
        tb.setTime(start);
        ib.setBegin(tb.build());
        tb.setTime(end);
        ib.setEnd(tb.build());

        // update period and session key
        TicketType.Ticket.Builder cstb = CST.toBuilder();
        cstb.setValidityPeriod(ib.build());

        // update TicketAuthenticatorWrapper
        AuthenticatorTicketType.AuthenticatorTicket.Builder atb = wrapper.toBuilder();
        atb.setTicket(EncryptionHelper.encrypt(CST, SSPrivateKey));

        return atb.build();
    }

    private void validateTicket(TicketType.Ticket ticket, AuthenticatorType.Authenticator authenticator) throws RejectedException {
        if (ticket.getClientId() == null) {
            throw new RejectedException("ClientId null in ticket");
        }
        if (authenticator.getClientId() == null) {
            throw new RejectedException("ClientId null in authenticator");
        }
        if (!authenticator.getClientId().equals(ticket.getClientId())) {
            throw new RejectedException("ClientIds do not match");
        }
        if (!this.isTimestampInInterval(authenticator.getTimestamp(), ticket.getValidityPeriod())) {
            throw new RejectedException("Session expired");
        }
    }
    
    private boolean isTimestampInInterval(TimestampType.Timestamp timestamp, IntervalType.Interval interval) {
        return true;
    }
}
