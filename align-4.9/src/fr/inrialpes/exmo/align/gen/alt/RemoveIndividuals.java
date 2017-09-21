/*
 * $Id: RemoveIndividuals.java 2120 2017-01-11 12:11:11Z euzenat $
 *
 * Copyright (C) INRIA, 2011-2012, 2015, 2017
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

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.ontology.Individual;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import fr.inrialpes.exmo.align.gen.Alterator;
import fr.inrialpes.exmo.align.gen.ParametersIds;


public class RemoveIndividuals extends BasicAlterator {

    public RemoveIndividuals( Alterator om ) {
	super( om );
    };

    public Alterator modify( Properties params ) {
	String p = params.getProperty( ParametersIds.REMOVE_INDIVIDUALS );
	if ( p == null ) return null;
	float percentage = Float.parseFloat( p );
        boolean isSubj, isObj;							//the individual can appear as subject or object
        List<Individual> individuals = modifiedModel.listIndividuals().toList();
        List<Individual> individualsTo = new ArrayList<Individual>();           //the list of individuals to be removed
        int nbIndividuals = individuals.size();					//the number of individuals
        int toBeRemoved = Math.round( percentage*nbIndividuals );                    //the number of individuals to be removed
        
        int [] n = this.randNumbers(nbIndividuals, toBeRemoved);                //build the list of individuals to be removed
        for ( int i=0; i<toBeRemoved; i++ ) {
            Individual indiv = individuals.get(n[i]);				//remove the individual from the reference alignment
            individualsTo.add( indiv );
            //alignment.remove( getLocalName( indiv.getURI() ) );
        }

        for ( Statement st : modifiedModel.listStatements().toList() ) {
            Resource subject   = st.getSubject();
            RDFNode object     = st.getObject();
            isSubj = isObj = false;

            if ( individualsTo.contains( subject ) )
                isSubj = true;
            if ( object.canAs( Resource.class ) )
                if ( individualsTo.contains( object.asResource() ) )
                   isObj = true;
            if ( isSubj )	//the individual appears as subject in the statement
                modifiedModel.remove( st );
            if ( isObj )	//the individual appears as object in the statement
                modifiedModel.remove( st );
        }
	return this; // useless
    };


}
