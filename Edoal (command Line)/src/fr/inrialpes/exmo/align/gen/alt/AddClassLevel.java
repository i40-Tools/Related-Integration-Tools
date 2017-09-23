/*
 * $Id: AddClassLevel.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA  2011-2013, 2015, 2017
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inrialpes.exmo.align.gen.Alterator;
import fr.inrialpes.exmo.align.gen.ParametersIds;

public class AddClassLevel extends BasicAlterator {
    final static Logger logger = LoggerFactory.getLogger( AddClassLevel.class );

    public AddClassLevel( Alterator om ) {
	super( om );
    };

    public Alterator modify( Properties params ) {
	String p = params.getProperty( ParametersIds.ADD_CLASSESLEVEL );
	if ( p == null ) return null;
	// JE: FIND A BETTER ENCODING (or several parameters)
	int index = p.indexOf(".");
	int level = Integer.parseInt( p.substring(0, index) );
	int nbClasses = Integer.parseInt( p.substring(index+1, p.length()) );
	logger.trace( "level : {}", level );
	logger.trace( "nbClasses : {}", nbClasses );
	//float percentage = 1.00f;
        //the parent class -> if level is 1 then we create a new class
        //else we get a random class from the level : level-1 to be the parent of the class
        OntClass parentClass;
        OntClass childClass;
        List<OntClass> parentClasses = new ArrayList<OntClass>();
        List<OntClass> childClasses = new ArrayList<OntClass>();

        buildClassHierarchy();                                                  //check if the class hierarchy is built
        if ( level == 1 ) {                                                     //the parent of the class is Thing, we add the class and then the rest of the classes
	    String classURI = getRandomString();
	    parentClass = modifiedModel.createClass( modifiedOntologyNS + classURI );//create a new class to the model
	    classHierarchy.addClass( modifiedOntologyNS + classURI, "Thing" );  //add the node in the hierarchy of classes
	    childClasses.add(parentClass);
        } else {
            parentClasses = classHierarchy.getClassesFromLevel( modifiedModel, level );
            //int nbParentClasses = parentClasses.size();                         //number of classes from the Ontology
            //int toAdd = Math.round( percentage*nbClasses );                       // 1 can be replaced by percentage
            for ( OntClass pClass : parentClasses ) {
                childClasses.add( addClass( pClass, getRandomString() ) );
            }
        }
        for ( OntClass pClass : childClasses ) {
	    String classURI = "IS_" + getLocalName( pClass.getURI() );
            for ( int i = 1; i < nbClasses; i++ ) {
                childClass = addClass( pClass, classURI );
                pClass = childClass;
            }	//this.classHierarchy.printClassHierarchy();
        }
	return this; // useless
    };

}
