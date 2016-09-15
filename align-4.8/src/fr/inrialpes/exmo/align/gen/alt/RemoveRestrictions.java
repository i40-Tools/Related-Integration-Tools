/*
 * $Id: RemoveRestrictions.java 2079 2015-10-16 19:00:16Z euzenat $
 *
 * Copyright (C) INRIA, 2011, 2015
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
import org.apache.jena.ontology.Restriction;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import fr.inrialpes.exmo.align.gen.Alterator;
import fr.inrialpes.exmo.align.gen.ParametersIds;

public class RemoveRestrictions extends BasicAlterator {

    public RemoveRestrictions( Alterator om ) {
	initModel( om );
    };

    public Alterator modify( Properties params ) {
	String p = params.getProperty( ParametersIds.REMOVE_RESTRICTIONS );
	if ( p == null ) return null;
	float percentage = Float.parseFloat( p );
        List<Restriction> restrictions   = new ArrayList<Restriction>();
        List<Restriction> restrictionsTo = new ArrayList<Restriction>();	//the array list of restrictions to be removed
        restrictions = modifiedModel.listRestrictions().toList();
        int nbRestrictions = restrictions.size();				//the number of restrictions
        int toBeRemoved = Math.round( percentage*nbRestrictions );			//the number of restrictions to be removed

        int [] n = this.randNumbers(nbRestrictions, toBeRemoved);		//build the list of restrictions to be removed
        for ( int i=0; i<toBeRemoved; i++ ) {
            Restriction res = restrictions.get(n[i]);
            restrictionsTo.add( res );
        }
        for ( Restriction res : restrictionsTo ) res.remove();
	return this; // useless
    };

}
