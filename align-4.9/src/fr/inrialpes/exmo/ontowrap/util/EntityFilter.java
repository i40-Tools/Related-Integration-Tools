/*
 * $Id: EntityFilter.java 1866 2014-01-14 14:34:59Z euzenat $
 *
 * Copyright (C) INRIA, 2010, 2013
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

package fr.inrialpes.exmo.ontowrap.util;

import java.net.URI;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

public class EntityFilter<T> extends FilteredSet<T> {
    final static Logger logger = LoggerFactory.getLogger( EntityFilter.class );
    private String ontoURI=null;
    private LoadedOntology<?> onto=null;
    
    public EntityFilter(Set<T> s, LoadedOntology<?> onto) {
	super(s);
	ontoURI = onto.getURI().toString().replaceFirst("#.*", "");
	this.onto=onto;
    }
    
    /**
     * filter ontology entities which have no URI or external URI 
     * ontoURI.equals(entURI.toString()) is for OWL API 1 : it seems that anonymous entities have their URI = ontology URI
     */
    protected boolean isFiltered(T obj) {
	try {
	    URI entURI=onto.getEntityURI(obj);
	    //logger.trace( "{}�- {}", entURI, ontoURI);
	    return (entURI.getAuthority()!=null) && (entURI==null || ontoURI.equals(entURI.toString()) || !entURI.toString().startsWith(ontoURI));
	}
	catch (OntowrapException e) {
	   logger.debug( "IGNORED: Entity is filtered", e );
	}
	return true;
    }
}
