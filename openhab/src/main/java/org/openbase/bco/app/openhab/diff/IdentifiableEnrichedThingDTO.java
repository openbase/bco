package org.openbase.bco.app.openhab.diff;

import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.jul.iface.Identifiable;

public class IdentifiableEnrichedThingDTO extends AbstractIdentifiableDTO<EnrichedThingDTO> implements Identifiable<String> {

    private final EnrichedThingDTO enrichedThingDTO;

    public IdentifiableEnrichedThingDTO(final EnrichedThingDTO enrichedThingDTO) {
        this.enrichedThingDTO = enrichedThingDTO;
    }

    @Override
    public String getId() {
        return enrichedThingDTO.UID;
    }

    @Override
    public EnrichedThingDTO getDTO() {
        return enrichedThingDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof IdentifiableEnrichedThingDTO)) {
            return false;
        }

        //TODO: apache equalsbuilder
        //TODO: verify that label is never null in openhab
        IdentifiableEnrichedThingDTO compared = (IdentifiableEnrichedThingDTO) o;
        return this.getDTO().label.equals(compared.getDTO().label) && compare(this.getDTO().location, compared.getDTO().location);
    }

    @Override
    public int hashCode() {
        //TODO: apache hashbuilder


        return super.hashCode();
    }

    private boolean compare(String a, String b) {
        if (a == null && b == null) {
            return true;
        }

        if (a == null) {
            return false;
        }

        if (b == null) {
            return false;
        }

        return a.equals(b);
    }
}

