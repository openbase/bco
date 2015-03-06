/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.view.struct.leave;

import rst.math.Vec3DFloatType;

/**
 *
 * @author thuxohl
 */
public class PositionContainer implements Leave {
    
    private Vec3DFloatType.Vec3DFloat position;

    public PositionContainer(Vec3DFloatType.Vec3DFloat position) {
        this.position = position;
    }

    @Override
    public Object getValue() {
        return position;
    }

    @Override
    public String getDescriptor() {
        return "Position";
    }
}
