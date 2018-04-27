package org.openbase.bco.app.openhab.diff;

import org.openbase.jul.iface.Identifiable;

import java.lang.reflect.Field;
import java.util.List;

public interface AbstractIdentifiableDTO<T> extends Identifiable<String> {

    T getDTO();
}
