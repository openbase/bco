package org.openbase.bco.registry.template.core.dbconvert;

/*-
 * #%L
 * BCO Registry Template Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import com.google.gson.JsonObject;
import org.openbase.bco.registry.lib.dbconvert.LabelDBConverter;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.AbstractDBVersionConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;

import java.io.File;
import java.util.Map;

/**
 * This db converter generates a service template label if non existent from an old version.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceTemplate_1_To_2_DBConverter extends AbstractDBVersionConverter {

    public static final String INTENSITY_STATE_SERVICE = "INTENSITY_STATE_SERVICE";

    public ServiceTemplate_1_To_2_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, final Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        if (outdatedDBEntry.has(UnitTemplate_4_To_5_DBConverter.TYPE_KEY)) {
            // get type string
            final String serviceTypeString = outdatedDBEntry.get(UnitTemplate_4_To_5_DBConverter.TYPE_KEY).getAsString();

            // is intensity state service so remove and do nothing
            if (serviceTypeString.equals(INTENSITY_STATE_SERVICE)) {
                removeFromDBSnapshot(outdatedDBEntry, dbSnapshot);
                return outdatedDBEntry;
            }

            // generate label if not existent
            if (!outdatedDBEntry.has(LabelDBConverter.LABEL_KEY)) {
                final ServiceType serviceType = ServiceType.valueOf(serviceTypeString);
                outdatedDBEntry = UnitTemplate_4_To_5_DBConverter.generateLabelForEnum(outdatedDBEntry, serviceType);
            }
        }

        return outdatedDBEntry;
    }
}
