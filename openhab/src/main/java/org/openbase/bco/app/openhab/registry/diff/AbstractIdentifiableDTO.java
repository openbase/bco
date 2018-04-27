package org.openbase.bco.app.openhab.registry.diff;

import org.openbase.jul.iface.Identifiable;

public interface AbstractIdentifiableDTO<T> extends Identifiable<String> {

    T getDTO();
}
