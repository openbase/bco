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
