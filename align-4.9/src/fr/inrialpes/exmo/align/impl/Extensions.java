/*
 * $Id: Extensions.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2009-2010, 2014, 2017
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

package fr.inrialpes.exmo.align.impl; 

import java.util.Hashtable;
import java.util.Collection;
import java.util.Set;
import java.lang.Cloneable;

/**
 * This contains the metadata extensions
 * They are indexed by namespace + local name.
 * Unfortunatelly there is no way to deal with this in a simple hastable with a pair as key.
 * So it is implemented the old way with concatenation... unfortunately
 */

public class Extensions implements Cloneable {

    private Hashtable<String,String[]> table;
 
    public Extensions() {
	table = new Hashtable<String,String[]>();
    }
  
    public Extensions( Hashtable<String,String[]> ht ) {
	table = ht;
    }
  
    public void setExtension( String ns, String label, String value ){
	final String[] ext = { ns, label, value };
	table.put( ns+label, ext );
    }

    public void unsetExtension( String ns, String label ){
	table.remove( ns+label );
    }

    public String getExtension( String ns, String label ){
	String[] ext = table.get( ns+label );
	if ( ext != null ) return ext[2];
	else return (String)null;
    }
    
    public String[] getExtensionCell( String ns, String label ){
	return table.get( ns+label );
    }
    
    public Set<String> keySet() {
	return table.keySet();
    }

    public Collection<String[]> getValues() {
	return table.values();
    }

    // JE2014: ideally we should convert them all, except those we do not...
    public Extensions convertExtension( String label, String method ) {
	Extensions newext = (Extensions)clone();
	String oldid = getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
	if ( oldid != null && !oldid.equals("") ) {
	    newext.setExtension( Namespace.EXT.uri, Annotations.DERIVEDFROM, oldid );
	    newext.unsetExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
	}
	String pretty = getExtension( Namespace.EXT.uri, Annotations.PRETTY );
	if ( pretty != null ){
	    newext.setExtension( Namespace.EXT.uri, Annotations.PRETTY, pretty+"/"+label );
	};
	if ( getExtension( Namespace.EXT.uri, Annotations.PROVENANCE ) != null ) {
	    newext.setExtension( Namespace.EXT.uri, Annotations.PROVENANCE,
				 getExtension( Namespace.EXT.uri, Annotations.PROVENANCE ) );
	}
	newext.setExtension( Namespace.EXT.uri, Annotations.METHOD, method );
	return newext;
    }

    @SuppressWarnings( "unchecked" )
    public Object clone() {
	
	return new Extensions( (Hashtable<String,String[]>)table.clone() ); //[W:unchecked]
    }    
}
