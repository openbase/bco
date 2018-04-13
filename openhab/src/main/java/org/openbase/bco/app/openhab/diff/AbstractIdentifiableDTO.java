package org.openbase.bco.app.openhab.diff;

import org.openbase.jul.iface.Identifiable;

import java.lang.reflect.Field;
import java.util.List;

public abstract class AbstractIdentifiableDTO<T> implements Identifiable<String> {

    public abstract T getDTO();

    @Override
    public boolean equals(Object o) {
        //TODO: this can be simplified to compare only fields which need to be updated between openhab and bco

        // test if the other object is null or not an AbstractIdentifiableDTO
        if (o == null || !(o instanceof AbstractIdentifiableDTO)) {
            return false;
        }

        AbstractIdentifiableDTO compared = (AbstractIdentifiableDTO) o;

        // test if the contained DTO type has the same class
        if (!this.getDTO().getClass().equals(compared.getDTO().getClass())) {
            return false;
        }

        return equalDTOTypes(this.getDTO(), compared.getDTO());
    }

    private boolean equalDTOTypes(final Object thisDTO, final Object comparedDTO) {
        // iterate over all fields of the DTO type and compare them
        for (final Field field : thisDTO.getClass().getFields()) {
            try {
                Object valueThis = null;
                try {
                    valueThis = field.get(thisDTO);
                } catch (NullPointerException ex) {
                    // thrown if field.get() returns null
                }

                Object valueCompared = null;
                try {
                    valueCompared = field.get(comparedDTO);
                } catch (NullPointerException ex) {
                    // thrown if field.get() returns null
                }

                if (!internalEquals(valueThis, valueCompared)) {
                    return false;
                }
            } catch (IllegalAccessException ex) {
                // field in both types not accessible so ignore it
            }
        }

        return true;
    }

    private boolean internalEquals(final Object fieldThis, final Object fieldCompared) {
        if (fieldThis == null && fieldCompared == null) {
            return true;
        }

        if (fieldThis != null && fieldCompared == null) {
            return false;
        }

        if (fieldThis == null && fieldCompared != null) {
            return false;
        }

        if (fieldThis instanceof List) {
            List listThis = (List) fieldThis;
            List listCompared = (List) fieldCompared;
            fieldThis.equals(fieldCompared);

            if (listThis.size() != listCompared.size()) {
                return false;
            } else {
                for (int i = 0; i < listThis.size(); i++) {
                    if (!equalDTOTypes(listThis.get(i), listCompared.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }

        return fieldThis.equals(fieldCompared);
    }

}
