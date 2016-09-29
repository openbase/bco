package org.openbase.bco.registry.lib.generator;

import com.google.protobuf.GeneratedMessage;
import java.util.UUID;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.IdGenerator;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M>
 */
public abstract class UUIDGenerator<M extends GeneratedMessage> implements IdGenerator<String, M> {

    @Override
    public String generateId(final M message) throws CouldNotPerformException {
        return UUID.randomUUID().toString();
    }
}
