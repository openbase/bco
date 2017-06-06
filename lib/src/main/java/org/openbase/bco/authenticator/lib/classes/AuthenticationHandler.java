/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.authenticator.lib.classes;

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
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SealedObject;
import javax.crypto.spec.SecretKeySpec;
import org.openbase.bco.authenticator.lib.iface.AuthenticationHandlerInterface;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.RejectedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.slf4j.LoggerFactory;
import rst.timing.IntervalType.Interval;
import rst.timing.TimestampType.Timestamp;
import rst.domotic.authentification.AuthenticatorTicketType.AuthenticatorTicket;
import rst.domotic.authentification.AuthenticatorType.Authenticator;
import rst.domotic.authentification.LoginResponseType.LoginResponse;
import rst.domotic.authentification.TicketType.Ticket;

/**
 *
 * @author sebastian
 */
public class AuthenticationHandler implements AuthenticationHandlerInterface {

    private static final String transformation = "AES";

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(AuthenticationHandler.class);

    public AuthenticationHandler() {

    }

    @Override
    public ByteString encryptObject(Serializable obj, byte[] key) throws RejectedException {
        try {
            // specify key
            SecretKeySpec sks = new SecretKeySpec(key, transformation);

            // create cipher
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, sks);
            SealedObject sealedObject = new SealedObject(obj, cipher);

            // cipher
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CipherOutputStream cos = new CipherOutputStream(baos, cipher);
            ObjectOutputStream outputStream = new ObjectOutputStream(cos);
            outputStream.writeObject(sealedObject);
            outputStream.close();

            return ByteString.copyFrom(baos.toByteArray());
        } catch (Exception ex) {
            Logger.getLogger(AuthenticationHandler.class.getName()).log(Level.SEVERE, null, ex);
            throw new RejectedException("Encryption did not work. TODO: Proper exception description & handling", ex);
        }
    }

    @Override
    public Object decryptObject(ByteString bstr, byte[] key) throws RejectedException {
        try {
            // specify key
            SecretKeySpec sks = new SecretKeySpec(key, transformation);

            // create cipher
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.DECRYPT_MODE, sks);

            // decipher
            ByteArrayInputStream bais = new ByteArrayInputStream(bstr.toByteArray());
            CipherInputStream cipherInputStream = new CipherInputStream(bais, cipher);
            ObjectInputStream inputStream = new ObjectInputStream(cipherInputStream);
            SealedObject sealedObject = (SealedObject) inputStream.readObject();

            return sealedObject.getObject(cipher);
        } catch (Exception ex) {
            Logger.getLogger(AuthenticationHandler.class.getName()).log(Level.SEVERE, null, ex);
            throw new RejectedException("Decryption did not work. TODO: Proper exception description & handling", ex);
        }
    }

    @Override
    public byte[] initKDCRequest(String clientPassword) {
        try {
            byte[] key = clientPassword.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            return Arrays.copyOf(key, 16);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
        return clientPassword.getBytes();
    }

    @Override
    public LoginResponse handleKDCRequest(String clientID, String clientNetworkAddress, byte[] TGSSessionKey, byte[] TGSPrivateKey) throws NotAvailableException, RejectedException {
        // find client's password in database
        String clientPassword = "password";

        // hash password
        byte[] clientPasswordHash = this.initKDCRequest(clientPassword);

        // set period
        long start = System.currentTimeMillis();
        long end = start + (5 * 60 * 1000);
        Interval.Builder ib = Interval.newBuilder();
        Timestamp.Builder tb = Timestamp.newBuilder();
        tb.setTime(start);
        ib.setBegin(tb.build());
        tb.setTime(end);
        ib.setEnd(tb.build());

        // create tgt
        Ticket.Builder tgtb = Ticket.newBuilder();
        tgtb.setClientId(clientID);
        tgtb.setClientIp(clientNetworkAddress);
        tgtb.setValidityPeriod(ib.build());
        tgtb.setSessionKey(TGSSessionKey.toString());

        // create TicketSessionKeyWrapper
        LoginResponse.Builder wb = LoginResponse.newBuilder();
        wb.setTicket(this.encryptObject(tgtb.build(), TGSPrivateKey));
        wb.setSessionKey(this.encryptObject(TGSSessionKey, clientPasswordHash));

        return wb.build();
    }

    @Override
    public List<Object> handleKDCResponse(String clientID, byte[] hashedClientPassword, LoginResponse wrapper) throws RejectedException {
        // decrypt TGS session key
        byte[] TGSSessionKey = (byte[]) this.decryptObject(wrapper.getSessionKey(), hashedClientPassword);

        // create Authenticator with empty timestamp
        // set timestamp in initTGSRequest()
        Timestamp.Builder tb = Timestamp.newBuilder();
        tb.setTime(System.currentTimeMillis());
        Authenticator.Builder ab = Authenticator.newBuilder();
        ab.setClientId(clientID);
        ab.setTimestamp(tb.build());

        // create TicketAuthenticatorWrapper
        AuthenticatorTicket.Builder atb = AuthenticatorTicket.newBuilder();
        atb.setAuthenticator(this.encryptObject(ab.build(), TGSSessionKey));
        atb.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<>();
        list.add(atb.build());
        list.add(TGSSessionKey);

        return list;
    }

    @Override
    public LoginResponse handleTGSRequest(byte[] TGSSessionKey, byte[] TGSPrivateKey, byte[] SSSessionKey, byte[] SSPrivateKey, AuthenticatorTicket wrapper) throws RejectedException {
        // decrypt ticket and authenticator
        Ticket CST = (Ticket) this.decryptObject(wrapper.getTicket(), TGSPrivateKey);
        Authenticator authenticator = (Authenticator) this.decryptObject(wrapper.getAuthenticator(), TGSSessionKey);

        // compare clientIDs and timestamp to period
        this.validateTicket(CST, authenticator);

        // set period
        long start = System.currentTimeMillis();
        long end = start + (5 * 60 * 1000);
        Interval.Builder ib = Interval.newBuilder();
        Timestamp.Builder tb = Timestamp.newBuilder();
        tb.setTime(start);
        ib.setBegin(tb.build());
        tb.setTime(end);
        ib.setEnd(tb.build());

        // update period and session key
        Ticket.Builder cstb = CST.toBuilder();
        cstb.setValidityPeriod(ib.build());
        cstb.setSessionKey(SSSessionKey.toString());

        // create TicketSessionKeyWrapper
        LoginResponse.Builder wb = LoginResponse.newBuilder();
        wb.setTicket(this.encryptObject(cstb.build(), SSPrivateKey));
        wb.setSessionKey(this.encryptObject(SSSessionKey, TGSSessionKey));

        return wb.build();
    }

    @Override
    public List<Object> handleTGSResponse(String clientID, byte[] TGSSessionKey, LoginResponse wrapper) throws RejectedException {
        // decrypt SS session key
        byte[] SSSessionKey = (byte[]) this.decryptObject(wrapper.getSessionKey(), TGSSessionKey);

        // create Authenticator with empty timestamp
        // set timestamp in initSSRequest()
        Authenticator.Builder ab = Authenticator.newBuilder();
        ab.setClientId(clientID);

        // create TicketAuthenticatorWrapper
        AuthenticatorTicket.Builder atb = AuthenticatorTicket.newBuilder();
        atb.setAuthenticator(this.encryptObject(ab.build(), SSSessionKey));
        atb.setTicket(wrapper.getTicket());

        // create wrapper list
        List<Object> list = new ArrayList<Object>();
        list.add(atb.build());
        list.add(SSSessionKey);

        return list;
    }

    @Override
    public AuthenticatorTicket initSSRequest(byte[] SSSessionKey, AuthenticatorTicket wrapper) throws RejectedException {
        // decrypt authenticator
        Authenticator authenticator = (Authenticator) this.decryptObject(wrapper.getAuthenticator(), SSSessionKey);

        // create Authenticator
        Timestamp.Builder tb = Timestamp.newBuilder();
        tb.setTime(System.currentTimeMillis());
        Authenticator.Builder ab = authenticator.toBuilder();
        ab.setTimestamp(tb.build());

        // create TicketAuthenticatorWrapper
        AuthenticatorTicket.Builder atb = wrapper.toBuilder();
        atb.setAuthenticator(this.encryptObject(ab.build(), SSSessionKey));

        return atb.build();
    }

    @Override
    public AuthenticatorTicket handleSSRequest(byte[] SSSessionKey, byte[] SSPrivateKey, AuthenticatorTicket wrapper) throws RejectedException {
        // decrypt ticket and authenticator
        Ticket CST = (Ticket) this.decryptObject(wrapper.getTicket(), SSPrivateKey);
        Authenticator authenticator = (Authenticator) this.decryptObject(wrapper.getAuthenticator(), SSSessionKey);

        // compare clientIDs and timestamp to period
        this.validateTicket(CST, authenticator);

        // set period
        long start = System.currentTimeMillis();
        long end = start + (5 * 60 * 1000);
        Interval.Builder ib = Interval.newBuilder();
        Timestamp.Builder tb = Timestamp.newBuilder();
        tb.setTime(start);
        ib.setBegin(tb.build());
        tb.setTime(end);
        ib.setEnd(tb.build());

        // update period and session key
        Ticket.Builder cstb = CST.toBuilder();
        cstb.setValidityPeriod(ib.build());

        // update TicketAuthenticatorWrapper
        AuthenticatorTicket.Builder atb = wrapper.toBuilder();
        atb.setTicket(this.encryptObject(CST, SSPrivateKey));

        return atb.build();
    }

    @Override
    public AuthenticatorTicket handleSSResponse(byte[] SSSessionKey, AuthenticatorTicket lastWrapper, AuthenticatorTicket currentWrapper) throws RejectedException {
        // decrypt authenticators
        Authenticator lastAuthenticator = (Authenticator) this.decryptObject(lastWrapper.getAuthenticator(), SSSessionKey);
        Authenticator currentAuthenticator = (Authenticator) this.decryptObject(currentWrapper.getAuthenticator(), SSSessionKey);

        // compare both timestamps
        
        this.validateTimestamp(lastAuthenticator.getTimestamp(), currentAuthenticator.getTimestamp());

        return currentWrapper;
    }

    private void validateTicket(Ticket ticket, Authenticator authenticator) throws RejectedException {
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

    private void validateTimestamp(Timestamp now, Timestamp then) throws RejectedException {
        if (now.getTime() != then.getTime()) {
            throw new RejectedException("Timestamps do not match");
        }
    }

    private boolean isTimestampInInterval(Timestamp timestamp, Interval interval) {
        return true;
    }

}
