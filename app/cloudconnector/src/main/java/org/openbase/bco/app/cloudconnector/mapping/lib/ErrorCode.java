package org.openbase.bco.app.cloudconnector.mapping.lib;

/*-
 * #%L
 * BCO Cloud Connector
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.jul.processing.StringProcessor;

/**
 * Enum mapping all error codes currently handled by google.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public enum ErrorCode {

    /**
     * Credentials have expired.
     */
    AUTH_EXPIRED,
    /**
     * General failure to authenticate.
     */
    AUTH_FAILURE,
    /**
     * The target is unreachable.
     */
    DEVICE_OFFLINE,
    /**
     * Internal timeout.
     */
    TIMEOUT,
    /**
     * The device is known to be turned hard off (if distinguishable from unreachable).
     */
    DEVICE_TURNED_OFF,
    /**
     * The device doesn't exist on the partner's side. This normally indicates a failure in data synchronization or a race condition.
     */
    DEVICE_NOT_FOUND,
    /**
     * The range in parameters is out of bounds.
     */
    VALUE_OUT_OF_RANGE,
    /**
     * The command or its parameters are unsupported (this should generally not happen, as traits and business logic should prevent it).
     */
    NOT_SUPPORTED,
    /**
     * Failure in processing the request.
     */
    PROTOCOL_ERROR,
    /**
     * Everything else, although anything that throws this should be replaced with a real error code.
     */
    UNKNOWN_ERROR;

    /**
     * Create representation of the error code.
     * This is done by converting the name of the error code to camel case and then setting
     * the first character to lower case.
     * E.g. DEVICE_TURNED_OFF becomes deviceTurnedOff
     *
     * @return a string representation of the error code.
     */
    @Override
    public String toString() {
        char[] charArray = StringProcessor.transformUpperCaseToPascalCase(name()).toCharArray();
        charArray[0] = Character.toLowerCase(charArray[0]);
        return new String(charArray);
    }
}
