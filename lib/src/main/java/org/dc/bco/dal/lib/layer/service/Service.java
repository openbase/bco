/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service;

import rst.homeautomation.service.ServiceConfigType.ServiceConfig;

/**
 *
 * @author Divine Threepwood
 */
public interface Service {
	public ServiceType getServiceType();
    public ServiceConfig getServiceConfig();
}
