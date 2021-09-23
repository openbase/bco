package org.openbase.bco.device.openhab.registry.diff;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openhab.core.io.rest.core.thing.EnrichedThingDTO;

public class IdentifiableEnrichedThingDTO implements AbstractIdentifiableDTO<EnrichedThingDTO> {

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

        IdentifiableEnrichedThingDTO compared = (IdentifiableEnrichedThingDTO) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(this.getDTO().UID, compared.getDTO().UID);
        equalsBuilder.append(this.getDTO().thingTypeUID, compared.getDTO().thingTypeUID);
        equalsBuilder.append(this.getDTO().label, compared.getDTO().label);
        equalsBuilder.append(this.getDTO().location, compared.getDTO().location);
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(this.getDTO().UID);
        hashCodeBuilder.append(this.getDTO().thingTypeUID);
        hashCodeBuilder.append(this.getDTO().label);
        hashCodeBuilder.append(this.getDTO().location);
        return hashCodeBuilder.toHashCode();
    }
}

