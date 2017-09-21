/**
 * $Id: AlteratorFactory.java 2120 2017-01-11 12:11:11Z euzenat $
 *
 * Copyright (C) INRIA, 2011, 2013-2015, 2017
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

package fr.inrialpes.exmo.align.gen;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

import org.semanticweb.owl.align.AlignmentException;

public class AlteratorFactory {
    final static Logger logger = LoggerFactory.getLogger( AlteratorFactory.class );

    // The parameter Ids should be here
    //public static final int ANY = 0;

    // This should be largely improved with precedence
    private static Map<String,String> alterators = null;

    public static Alterator newInstance( String id, Alterator om ) {
	//logger.trace( ">>>>>>>> {}", id );
	if ( alterators == null ) init();
	String classname = alterators.get( id );
	Alterator alt = null;
	if ( classname != null ) {
	    try {
		// This should also be a static getInstance!
		Class<?> altClass = Class.forName(classname);
		Class<?>[] cparams = {Class.forName("fr.inrialpes.exmo.align.gen.Alterator")};
		Constructor<?> altConstructor = altClass.getConstructor(cparams);
		Object[] mparams = { om };
		alt = (Alterator)altConstructor.newInstance(mparams);
	    } catch (ClassNotFoundException cnfex ) {
		logger.debug( "IGNORED Exception", cnfex ); // better raise errors
	    } catch (NoSuchMethodException nsmex) {
		logger.debug( "IGNORED Exception", nsmex );
	    } catch (InstantiationException ieex) {
		logger.debug( "IGNORED Exception", ieex );
	    } catch (IllegalAccessException iaex) {
		logger.debug( "IGNORED Exception", iaex );
	    } catch (InvocationTargetException itex) {
		logger.debug( "IGNORED Exception", itex );
	    }
	}
	return alt;
    }

    public static void init() {
	alterators = new HashMap<String, String>();
	alterators.put( ParametersIds.REMOVE_CLASSES, "fr.inrialpes.exmo.align.gen.alt.RemoveClasses" );
	alterators.put( ParametersIds.REMOVE_PROPERTIES, "fr.inrialpes.exmo.align.gen.alt.RemoveProperties" );
	alterators.put( ParametersIds.REMOVE_RESTRICTIONS, "fr.inrialpes.exmo.align.gen.alt.RemoveRestrictions" );
	alterators.put( ParametersIds.REMOVE_COMMENTS, "fr.inrialpes.exmo.align.gen.alt.RemoveComments" );
	alterators.put( ParametersIds.RENAME_CLASSES, "fr.inrialpes.exmo.align.gen.alt.RenameClasses" );
	alterators.put( ParametersIds.RENAME_PROPERTIES, "fr.inrialpes.exmo.align.gen.alt.RenameProperties" );
	alterators.put( ParametersIds.REMOVE_INDIVIDUALS, "fr.inrialpes.exmo.align.gen.alt.RemoveIndividuals" );
	alterators.put( ParametersIds.REMOVE_CLASSESLEVEL, "fr.inrialpes.exmo.align.gen.alt.RemoveClassLevel" );
	alterators.put( ParametersIds.LEVEL_FLATTENED, "fr.inrialpes.exmo.align.gen.alt.FlattenLevel" );
	alterators.put( ParametersIds.ADD_CLASSES, "fr.inrialpes.exmo.align.gen.alt.AddClasses" );
	alterators.put( ParametersIds.ADD_PROPERTIES, "fr.inrialpes.exmo.align.gen.alt.AddProperties" );
	alterators.put( ParametersIds.ADD_CLASSESLEVEL, "fr.inrialpes.exmo.align.gen.alt.AddClassLevel" );
	alterators.put( ParametersIds.NO_HIERARCHY, "fr.inrialpes.exmo.align.gen.alt.SuppressHierarchy" );
	//alterators.put( , "fr.inrialpes.exmo.align.gen.alt." );
	//alterators.put( , "fr.inrialpes.exmo.align.gen.alt." );
	//alterators.put( , "fr.inrialpes.exmo.align.gen.alt." );
    }

    public static void declareAlterator( String id, String classname ) {
	if ( alterators == null ) init();
	alterators.put( id, classname );
    }

    // I should add a run primitive
    // which executes all the parameters in the given order
    public static Alterator cascadeAlterators( Alterator init, Properties params ) throws AlignmentException {
	Alterator modifier = init;
	// JE: Of course, this could be improved (at least be rendered generic)
	if ( params.getProperty( ParametersIds.REMOVE_CLASSES ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.REMOVE_CLASSES, modifier );
	    modifier.modify( params );
	}
	if ( params.getProperty( ParametersIds.REMOVE_PROPERTIES ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.REMOVE_PROPERTIES, modifier );
	    modifier.modify( params );
	}
	if ( params.getProperty( ParametersIds.REMOVE_RESTRICTIONS ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.REMOVE_RESTRICTIONS, modifier );
	    modifier.modify( params );
	}
	if ( params.getProperty( ParametersIds.REMOVE_COMMENTS ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.REMOVE_COMMENTS, modifier );
	    modifier.modify( params );
	}

	if ( params.getProperty( ParametersIds.RENAME_CLASSES ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.RENAME_CLASSES, modifier );
	    modifier.modify( params );
	}
	if ( params.getProperty( ParametersIds.RENAME_PROPERTIES ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.RENAME_PROPERTIES, modifier );
	    modifier.modify( params );
	}
	// UNTIL HERE, WE USE THE DOCUMENTED ORDER

	if ( params.getProperty( ParametersIds.REMOVE_INDIVIDUALS ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.REMOVE_INDIVIDUALS, modifier );
	    modifier.modify( params );
	}
	if ( params.getProperty( ParametersIds.REMOVE_CLASSESLEVEL ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.REMOVE_CLASSESLEVEL, modifier );
	    modifier.modify( params );
	}
	if ( params.getProperty( ParametersIds.LEVEL_FLATTENED ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.LEVEL_FLATTENED, modifier );
	    modifier.modify( params );
	}
	if ( params.getProperty( ParametersIds.ADD_CLASSES ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.ADD_CLASSES, modifier );
	    modifier.modify( params );
	}
	if ( params.getProperty( ParametersIds.ADD_PROPERTIES ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.ADD_PROPERTIES, modifier );
	    modifier.modify( params );
	}

	if ( params.getProperty( ParametersIds.ADD_CLASSESLEVEL ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.ADD_CLASSESLEVEL, modifier );
	    modifier.modify( params );
	}
	if ( params.getProperty( ParametersIds.NO_HIERARCHY ) != null ) {
	    modifier = AlteratorFactory.newInstance( ParametersIds.NO_HIERARCHY, modifier );
	    modifier.modify( params );
	}
	/*
        for( Entry<Object,Object> m : params.entrySet() ) {
	    //logger.trace( "[{}] = [{}]", m.getKey(), m.getValue );
	    modifier.modifyOntology( m.getKey(), m.getValue );					//modify the ontology according to it
        }
	*/
	return modifier;
    }
}
