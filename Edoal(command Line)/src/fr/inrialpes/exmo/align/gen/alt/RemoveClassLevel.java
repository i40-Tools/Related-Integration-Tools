/*
 * $Id: RemoveClassLevel.java 2120 2017-01-11 12:11:11Z euzenat $
 *
 * Copyright (C) INRIA, 2011-2013, 2015, 2017
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

package fr.inrialpes.exmo.align.gen.alt;

import org.apache.jena.ontology.OntClass;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inrialpes.exmo.align.gen.Alterator;
import fr.inrialpes.exmo.align.gen.ParametersIds;

public class RemoveClassLevel extends BasicAlterator {
    final static Logger logger = LoggerFactory.getLogger( RemoveClassLevel.class );

    public RemoveClassLevel( Alterator om ) {
	super( om );
    };

    public Alterator modify( Properties params ) {
	String p = params.getProperty( ParametersIds.REMOVE_CLASSESLEVEL );
	if ( p == null ) return null;
	int level = Integer.parseInt( p );
        HashMap<String, String> uris = new HashMap<String, String>();
        //logger.trace( "Level {}", level );
        buildClassHierarchy();							//build the class hierarchy if necessary
	for ( OntClass cl : classHierarchy.getClassesFromLevel( modifiedModel, level ) ) {                                //remove the classes from the hierarchy
            String parentURI = removeClass( cl );
            uris.put( cl.getURI(), parentURI );
	}
        //checks if the class appears like unionOf .. and replaces its appearence with the superclass
        modifiedModel = changeDomainRange( uris );
	return this; // useless
    };

}
