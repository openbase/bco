package org.openbase.bco.app.cloud.connector.mapping.lib;

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
        char charArray[] = StringProcessor.transformToUpperCase(name()).toCharArray();
        charArray[0] = Character.toLowerCase(charArray[0]);
        return new String(charArray);
    }
}
