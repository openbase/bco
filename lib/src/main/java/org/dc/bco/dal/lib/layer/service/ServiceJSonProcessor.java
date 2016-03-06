/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine
 * Threepwood</a>
 */
public class ServiceJSonProcessor {

    private final JsonParser parser;
    private final Gson gson;

    public ServiceJSonProcessor() {
        this.parser = new JsonParser();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public String serialize(final Service service, final ServiceTemplate.ServiceType type) throws CouldNotPerformException {
        Object serviceAtribute = null;
        try {
            String methodName = ("get" + StringProcessor.transformUpperCaseToCamelCase(type.toString())).replaceAll("Service", "");
            Method method = service.getClass().getMethod(methodName);
            serviceAtribute = method.invoke(service);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new CouldNotPerformException(ex);
        }
        if (serviceAtribute == null) {
            throw new CouldNotPerformException("Null value for service[" + service.toString() + "] and type [" + type + "]");
        }
        if (serviceAtribute instanceof Message) {
            try {
                String jsonStringRep = JsonFormat.printToString((Message) serviceAtribute);

                // format
                JsonElement el = parser.parse(jsonStringRep);
                return gson.toJson(el);

            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not serialize service argument to string!", ex);
            }
        } else {
            return serviceAtribute.toString();
        }
    }
    
//
//    public Object deserialize(String jsonStringRep) throws CouldNotPerformException {
//        try {
//            JsonFormat.merge(jsonStringRep, builder);
//            return transformer.transform((M) builder.build());
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not deserialize " + file + " into " + builder + "!", ex);
//        }
//    }
}
