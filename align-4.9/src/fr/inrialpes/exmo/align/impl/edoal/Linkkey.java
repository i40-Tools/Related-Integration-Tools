/*
 * $Id: Linkkey.java 2101 2015-11-20 23:16:46Z euzenat $
 *
 * Copyright (C) INRIA, 2015
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package fr.inrialpes.exmo.align.impl.edoal;

import fr.inrialpes.exmo.align.impl.Extensions;
import fr.inrialpes.exmo.align.impl.Extensible;

import fr.inrialpes.exmo.align.parser.TypeCheckingVisitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owl.align.AlignmentException;

/**
 *
 * @author Nicolas Guillouet (nicolas@meaningengines.com)
 */
public class Linkkey implements Extensible {
    
    private Set<LinkkeyBinding> bindings;
    protected Extensions extensions = null;
    
    public void accept(EDOALVisitor visitor) throws AlignmentException {
        visitor.visit(this);
    }
    
    public TypeCheckingVisitor.TYPE accept( TypeCheckingVisitor visitor ) throws AlignmentException {
	return visitor.visit(this);
    }

    public Linkkey() {
        bindings = new HashSet<>();
    }

    public void addBinding(LinkkeyBinding binding){
        bindings.add(binding);
    }
    
    public Set<LinkkeyBinding> bindings(){
        return bindings;
    }
    
    public Collection<String[]> getExtensions() { 
    	if ( extensions != null ) return extensions.getValues();
    	else return null;
    }

    public void setExtensions( Extensions p ){
	extensions = p;
    }

    public void setExtension( String uri, String label, String value ) {
	if ( extensions == null ) extensions = new Extensions();
	extensions.setExtension( uri, label, value );
    };

    public String getExtension( String uri, String label ) {
	if ( extensions != null ) {
	    return extensions.getExtension( uri, label );
	} else {
	    return (String)null;
	}
    };

    public Linkkey inverse() {
	Linkkey lk = new Linkkey();
	for ( LinkkeyBinding bind : bindings ) {
	    lk.addBinding( bind.inverse() );
	}
	if ( extensions != null ) lk.setExtensions( extensions );
	return lk;
    };

}
