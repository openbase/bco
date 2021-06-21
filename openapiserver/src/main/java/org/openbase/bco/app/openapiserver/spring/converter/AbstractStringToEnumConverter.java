package org.openbase.bco.app.openapiserver.spring.converter;

/*-
 * #%L
 * BCO OpenAPI Server
 * %%
 * Copyright (C) 2018 - 2021 openbase.org
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

import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class AbstractStringToEnumConverter<E extends Enum> implements Converter<String, E> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Class<E> enumClass;
    private Method valueOfMethod;

    private AbstractStringToEnumConverter() {
        // make constructor private so that it cannot be invoked
    }

    protected AbstractStringToEnumConverter(final Class<E> enumClass) {
        this.enumClass = enumClass;
        try {
            valueOfMethod = enumClass.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException e) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException("EnumClass[" + enumClass.getSimpleName() + "] does not have a valid valueOf method!", this, e), logger);
            valueOfMethod = null;
        }
    }

    @Override
    public E convert(String source) {
        try {
            return (E) valueOfMethod.invoke(null, source);
        } catch (NullPointerException | IllegalAccessException | InvocationTargetException e) {
            ExceptionPrinter.printHistory(new FatalImplementationErrorException("Converter could not extract a valid valueOf method!", this, e), logger);
            return null;
        }
    }
}
