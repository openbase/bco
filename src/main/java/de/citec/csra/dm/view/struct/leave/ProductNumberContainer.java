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
public class ProductNumberContainer implements Leave {

    final String productNumber;

    public ProductNumberContainer(String productNumber) {
        this.productNumber = productNumber;
    }

    @Override
    public String getDescriptor() {
        return "Product Number";
    }

    @Override
    public Object getValue() {
        return productNumber;
    }

}
