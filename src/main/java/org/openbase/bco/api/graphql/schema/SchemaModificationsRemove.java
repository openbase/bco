package org.openbase.bco.api.graphql.schema;

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
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

import com.google.api.graphql.rejoiner.SchemaModification;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.api.graphql.rejoiner.Type;
import com.google.api.graphql.rejoiner.TypeModification;
import org.openbase.type.domotic.unit.UnitConfigType;

/**
 * Bundle all schema modifications to remove fields from protobuf types.
 *
 * Note:
 * This is needed because there are issues with replacing fields otherwise.
 * Rejoiner has a method to replace fields, but in this case you have to transform
 * the field value through coercing and then you cannot access the context of the
 * request (token, language, ...). Therefore, the fields have to be removed (below)
 * and can then be added again (see SchemaModificationsAdd). Since there is
 * an exception thrown if an existing field is added, the removal has to be performed
 * first. Thus, removal and adding is split into two different classes and the
 * removal module is registered before the modifications module (see
 * BCOGraphQlApiSpringBootApplication).
 */
public class SchemaModificationsRemove extends SchemaModule {

    @SchemaModification
    TypeModification removeMultiLanguageFields = Type.find(UnitConfigType.UnitConfig.getDescriptor()).removeFields("description", "label");
}
