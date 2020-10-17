package org.openbase.bco.api.graphql.coercing;

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
