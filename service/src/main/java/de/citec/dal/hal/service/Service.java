/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import rst.homeautomation.service.ServiceConfigType;

/**
 *
 * @author Divine Threepwood
 */
public interface Service {
	public ServiceType getServiceType();
    public ServiceConfigType.ServiceConfig getServiceConfig();
}
