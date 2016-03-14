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
import com.google.gson.JsonParser;
import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
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

                // formatting only adds empty lines
                // format
                //JsonElement el = parser.parse(jsonStringRep);
                return jsonStringRep;

            } catch (Exception ex) {
                throw new CouldNotPerformException("Could not serialize service argument to string!", ex);
            }
        } else {
            Double d;
            return serviceAtribute.toString();
        }
    }

    public Class getServiceAttributeType(final Service service, final ServiceTemplate.ServiceType type) throws CouldNotPerformException {
        Object serviceAtribute = null;
        try {
            String methodName = ("get" + StringProcessor.transformUpperCaseToCamelCase(type.toString())).replaceAll("Service", "");
            Method method = service.getClass().getMethod(methodName);
            serviceAtribute = method.invoke(service);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new CouldNotPerformException(ex);
        }
        return serviceAtribute.getClass();
    }

    /**
     * TODO: Write an enum to map from java primitve types to protobuf primitves
     * and use these as service attribute types. When parsing use the
     * FieldDescriptor.Type.valueOf on the service attribute type and get the
     * according java type. The according class should be
     * java.lang.(javaTypeToString). Then this primitive should have a
     * constructor with a string as a parameter. Call this to get the java
     * value. In the end write unit tests to serialize and deserialize alle java
     * primitive and one example protobuf message. Serializing and deserialzing
     * should not change the object at all.
     */
    public Object deserialize(String jsonStringRep, String serviceAttributeType) throws CouldNotPerformException {
        Object obj = null;
        try {
            Class attibuteClass = Class.forName(serviceAttributeType);
            System.out.println("Found class [" + attibuteClass.getSimpleName() + "]");
            System.out.println("Number of constructors [" + attibuteClass.getConstructors().length + "]");
            System.out.println("Number of declared inner classes [" + attibuteClass.getClasses().length + "]");
            for (int i = 0; i < attibuteClass.getClasses().length; i++) {
                System.out.println("InnerClass [" + attibuteClass.getClasses()[i].getName() + "]");
            }
            if (attibuteClass.getClasses().length > 0) {
                Class builderClass = attibuteClass.getClasses()[0];
                try {
                    Message.Builder builder = (Message.Builder) attibuteClass.getMethod("newBuilder").invoke(null);
                    JsonFormat.merge(jsonStringRep, builder);
                    obj = builder.build();
                    System.out.println("Parsed builder [" + obj + "]");
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(ServiceJSonProcessor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(ServiceJSonProcessor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ServiceJSonProcessor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ServiceJSonProcessor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ServiceJSonProcessor.class.getName()).log(Level.SEVERE, null, ex);
                } catch (JsonFormat.ParseException ex) {
                    Logger.getLogger(ServiceJSonProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                // The simple types often offer a constructor by string
                Constructor constructor = attibuteClass.getConstructor(jsonStringRep.getClass());
                obj = constructor.newInstance(jsonStringRep);
                System.out.println("Succesfully created instance of [" + obj.getClass().getSimpleName() + "] with value [" + obj + "]");
            } catch (NoSuchMethodException ex) {
                System.out.println("Class has no copy constructor");
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                Logger.getLogger(ServiceJSonProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
//            attibuteClass.getConstructor().

//            attibuteClass.getConstructor().
        } catch (ClassNotFoundException ex) {
            throw new CouldNotPerformException("Could not find class from ServiceAttributeType [" + serviceAttributeType + "]", ex);
        }

//        try {
//            JsonFormat.merge(jsonStringRep, builder);
//            return transformer.transform((M) builder.build());
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not deserialize " + file + " into " + builder + "!", ex);
//        }
        return obj;
    }
}
