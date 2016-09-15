/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inrialpes.exmo.align.impl;

import fr.inrialpes.exmo.align.impl.Extensions;
import java.util.Collection;

/**
 *
 * @author Nicolas Guillouet <nicolas@meaningengines.com>
 */

public interface Extensible {
    
    /**
     * Extensions are a way to read and add other information (metadata)
     * to the alignment structure itself.
     * getExtensions returns a set of tripes: uri*label*value
     * all three being String
     */
    public Collection<String[]> getExtensions();
    public String getExtension( String uri, String label );
    public void setExtension( String uri, String label, String value );
    public void setExtensions( Extensions p );
}
