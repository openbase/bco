/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.jul.extension.rsb.com.AbstractIdentifiableRemote;
import java.util.HashMap;

/**
 *
 * @author mpohling
 */
public class ServiceRemoteMap<S extends Service, SR extends AbstractIdentifiableRemote & Service> extends HashMap<String, SR> {
    
}
