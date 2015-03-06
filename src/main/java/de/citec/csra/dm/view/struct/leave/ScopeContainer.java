/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leave;

import rst.rsb.ScopeType;

/**
 *
 * @author thuxohl
 */
public class ScopeContainer implements Leave {
    
    private final ScopeType.Scope scope;

    public ScopeContainer(ScopeType.Scope scope) {
        this.scope = scope;
    }

    @Override
    public Object getValue() {
        return this.scope;
    }

    @Override
    public String getDescriptor() {
        return "Scope";
    } 
}
