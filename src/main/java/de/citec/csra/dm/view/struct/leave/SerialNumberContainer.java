/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leave;

/**
 *
 * @author thuxohl
 */
public class SerialNumberContainer implements Leave {
    
    String serialNumber;

    public SerialNumberContainer(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @Override
    public Object getValue() {
        return serialNumber;
    }

    @Override
    public String getDescriptor() {
        return "Serial Number";
    }
}
