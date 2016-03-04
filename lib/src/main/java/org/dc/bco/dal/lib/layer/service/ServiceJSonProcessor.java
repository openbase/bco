/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class ServiceJSonProcessor {

    private final JsonParser parser;
    private final Gson gson;

    public ServiceJSonProcessor() {
        this.parser = new JsonParser();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

//    public String serialize(final Service service, final ServiceTemplate.ServiceType type) throws CouldNotPerformException {
//        try {
//
//            Object serviceAtribute; // TODO Tamino insert service atribute
//
//            String jsonStringRep = JsonFormat.printToString(serviceAtribute);
//
//            // format
//            JsonElement el = parser.parse(jsonStringRep);
//            return gson.toJson(el);
//
//        } catch (Exception ex) {
//            throw new CouldNotPerformException("Could not serialize service argument to string!", ex);
//        }
//    }
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
