package org.openbase.bco.api.graphql.coercing;

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

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.language.LabelType;

public class LabelTypeCoercing implements Coercing<LabelType.Label, String> {
    @Override
    public String serialize(Object o) throws CoercingSerializeException {
        System.out.println("Serialize with unit label transformer..." + o.toString());
        try {
            return LabelProcessor.getBestMatch((LabelType.Label) o);
        } catch (NotAvailableException e) {
            return o.toString();
        }
    }

    @Override
    public LabelType.Label parseValue(Object o) throws CoercingParseValueException {
        System.out.println("Parse value called" + o.toString());
        LabelType.Label.Builder builder = LabelType.Label.newBuilder();
        builder.addEntryBuilder().setKey("en").addValue(o.toString());
        return builder.build();
    }

    @Override
    public LabelType.Label parseLiteral(Object o) throws CoercingParseLiteralException {
        System.out.println("parse literal called: " + o.toString());
        return parseValue(o);
    }
}
