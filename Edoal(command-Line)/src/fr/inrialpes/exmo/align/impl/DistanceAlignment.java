/*
 * $Id: DistanceAlignment.java 2132 2017-04-19 12:44:01Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2011, 2013-2015, 2017
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

package fr.inrialpes.exmo.align.impl; 

import java.lang.ClassNotFoundException;
import java.lang.ClassCastException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Comparator;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.ontowrap.OntowrapException;

import fr.inrialpes.exmo.ontosim.util.HungarianAlgorithm;

/**
 * The mother class for distance or similarity-based alignments.
 * It is abstract because it does not provide an implemented similarity measure
 * Otherwise everything is fine.
 *
 * This class should work with similarity and distances, as soon as, the used
 * similarity structure is defined as such.
 *
 * @author Jérôme Euzenat
 * @version $Id: DistanceAlignment.java 2132 2017-04-19 12:44:01Z euzenat $ 
 */

public abstract class DistanceAlignment extends ObjectAlignment implements AlignmentProcess {
    final static Logger logger = LoggerFactory.getLogger( DistanceAlignment.class );
    Similarity sim;

    /** Creation **/
    public DistanceAlignment() {};

    public void setSimilarity( Similarity s ) { sim = s; }
    public Similarity getSimilarity() { return sim; }

    /**
     * Process matching
     * - create distance data structures,
     * - compute distance or similarity
     * - extract alignment
     **/
    public void align( Alignment alignment, Properties params ) throws AlignmentException {
	if ( alignment != null ) loadInit( alignment );
	if ( params.getProperty("type") != null ) setType( params.getProperty("type") );
	// This is a 1:1 alignment in fact
	else if ( type == null ) setType("11");
	if ( sim == null )
	    throw new AlignmentException("DistanceAlignment: requires a similarity measure");

	sim.initialize( getOntologyObject1(), getOntologyObject2(), init );
	sim.compute( params );
	if ( params.getProperty("printMatrix") != null ) printDistanceMatrix(params);
	extract( getType(), params );
    }

    /**
     * Prints the distance matrix
     *
     * @param params: the parameters of the distance
     */
    public void printDistanceMatrix( Properties params ){
	String algName = params.getProperty("algName");
	String metric = "distance";
	if ( sim.getSimilarity() ) metric = "similarity";
	if ( algName == null ) algName = getClass().toString();
	System.out.println("\\documentclass{article}\n");
	System.out.println("\\usepackage{graphics}\n");
	System.out.println("\\begin{document}\n");
	System.out.println("\\begin{table}");
	sim.printClassSimilarityMatrix("tex");
	System.out.println("\\caption{Class "+metric+" with measure "+algName+".}" );
	System.out.println("\\end{table}");
	System.out.println();
	System.out.println("\\begin{table}");
	sim.printPropertySimilarityMatrix("tex");
	System.out.println("\\caption{Property "+metric+" with measure "+algName+".}" );
	System.out.println("\\end{table}");
	System.out.println();
	System.out.println("\\begin{table}");
	sim.printIndividualSimilarityMatrix("tex");
	System.out.println("\\caption{Individual "+metric+" with measure "+algName+".}" );
	System.out.println("\\end{table}");
	System.out.println("\n\\end{document}");
    }

    /**
     * Suppresses the distance matrix
     */
    public void cleanUp() {
	sim = null;
    }

    /**
     * Extract the alignment form the Similarity
     *
     * @param type: the type of alignment to extract:
     * There are theoretically 16 types of extractors composing the
     * characteristics
     * [q]estion mark = ?, one or zero relation
     * [s]tar = *, one, zero or many relations
     * [1] = 1, exactly one relation
     * [p]lus = +, one or many relations
     * for each place of the relation. Howerver, since it is not possible from a matrics to guarantee that one object will be in at least one relation, this is restricted to the four following types:
     * ?? (covering 11, 1? and ?1)
     * ** (covering ++, *+ and +*)
     * ?* (covering 1*, 1+ and ?+)
     * *? (covering +?, *1 and +1)
     * @param params: the parameter of the measure
     * @throws AlignmentException when something goes wrong
     * @return the extracted alignment
     */
    public Alignment extract(String type, Properties params) throws AlignmentException {
	double threshold = 0.;
	if (  params.getProperty("threshold") != null )
	    threshold = Double.parseDouble( params.getProperty("threshold") );

	//logger.trace("The type is {} with length = {}", type, type.length() );
	if ( type.equals("?*") || type.equals("1*") || type.equals("?+") || type.equals("1+") ) return extractqs( threshold, params );
	else if ( type.equals("??") || type.equals("1?") || type.equals("?1") || type.equals("11") ) return extractqq( threshold, params );
	else if ( type.equals("*?") || type.equals("+?") || type.equals("*1") || type.equals("+1") ) return extractsq( threshold, params );
	else if ( type.equals("**") || type.equals("+*") || type.equals("*+") || type.equals("++") ) return extractss( threshold, params );
	// The else should be an error message
	else throw new AlignmentException("Unknown alignment type: "+type);
    }

    // JE: It is now certainly possible to virtualise extraction as it has
    // been done for printing matrix in MatrixMeasure (todo)

    /**
     * Extract the alignment of a ?* type
     * Non symmetric: for each entity of onto1, take the highest if superior to threshold
     * Complexity: O(n^2)
     * 
     * @param threshold: the threshold to apply to extracted correspondences
     * @param params: the parameter of the measure
     * @return the extracted alignment
     */
    // JE: Yes, could only be implemented by copying the code...
    public Alignment extractsq( double threshold, Properties params) {
	return extractqs( threshold, params);
    }
    @SuppressWarnings({"unchecked","rawTypes"}) //ConcatenatedIterator
    public Alignment extractqs( double threshold, Properties params) {
      double max = 0.;
      boolean found = false;
      double val = 0.;

      try {
	  // Extract for properties
	  ConcatenatedIterator<Object> pit1 = new 
	      ConcatenatedIterator<Object>(getOntologyObject1().getObjectProperties().iterator(),
				   getOntologyObject1().getDataProperties().iterator());
	  for( Object prop1 : pit1 ){
	      found = false; max = threshold; val = 0.;
	      Object prop2 = null;
	      ConcatenatedIterator<Object> pit2 = new 
		  ConcatenatedIterator<Object>(getOntologyObject2().getObjectProperties().iterator(),
				       getOntologyObject2().getDataProperties().iterator());
	      for ( Object current : pit2 ){
		  if ( sim.getSimilarity() ) val = sim.getPropertySimilarity(prop1,current);
		  else val =  1. - sim.getPropertySimilarity(prop1,current);
		  if ( val > max) {
		      found = true; max = val; prop2 = current;
		  }
	      }
	      if ( found ) addAlignCell(prop1,prop2, "=", max);
	  }
	  // Extract for classes
	  for ( Object class1 : getOntologyObject1().getClasses() ) {
	      found = false; max = threshold; val = 0;
	      Object class2 = null;
	      for ( Object current : getOntologyObject2().getClasses() ) {
		  if ( sim.getSimilarity() ) val = sim.getClassSimilarity(class1,current);
		  else val = 1. - sim.getClassSimilarity(class1,current);
		  if (val > max) {
		      found = true; max = val; class2 = current;
		  }
	      }
	      if ( found ) addAlignCell( class1, class2, "=", max );
	  }
	  // Extract for individuals
	  if (  params.getProperty("noinst") == null ){
	      for ( Object ind1 : getOntologyObject1().getIndividuals() ) {
		  if ( getOntologyObject1().getEntityURI( ind1 ) != null ) {
		      found = false; max = threshold; val = 0;
		      Object ind2 = null;
		      for ( Object current : getOntologyObject2().getIndividuals() ) {
			  if ( getOntologyObject2().getEntityURI( current ) != null ) {
			      if ( sim.getSimilarity() ) val = sim.getIndividualSimilarity( ind1, current );
			      else val = 1 - sim.getIndividualSimilarity( ind1, current );
			      if (val > max) {
				  found = true; max = val; ind2 = current;
			      }
			  }
		      }
		      if ( found ) addAlignCell(ind1,ind2, "=", max);
		  }
	      }
	  }
      } catch (OntowrapException owex) { 
	  logger.debug( "IGNORED Exception", owex );
      } catch (AlignmentException alex) { 
	  logger.debug( "IGNORED Exception", alex );
      }
      return((Alignment)this);
    }

    /**
     * Extract the alignment of a ** type
     * Symmetric: return all elements above threshold
     * Complexity: O(n^2)
     * 
     * @param threshold: the threshold to apply to extracted correspondences
     * @param params: the parameter of the measure
     * @return the extracted alignment
     */
    @SuppressWarnings({"unchecked","rawTypes"}) //ConcatenatedIterator
    public Alignment extractss( double threshold, Properties params) {
	double val = 0.;
	try {
	    // Extract for properties
	    ConcatenatedIterator<Object> pit1 = new 
		ConcatenatedIterator<Object>(getOntologyObject1().getObjectProperties().iterator(),
				     getOntologyObject1().getDataProperties().iterator());
	    for( Object prop1 : pit1 ){
		ConcatenatedIterator<Object> pit2 = new 
		    ConcatenatedIterator<Object>(getOntologyObject2().getObjectProperties().iterator(),
					 getOntologyObject2().getDataProperties().iterator());
		for ( Object prop2 : pit2 ){
		    if ( sim.getSimilarity() ) val = sim.getPropertySimilarity(prop1,prop2);
		    else val =  1. - sim.getPropertySimilarity(prop1,prop2);
		    if ( val > threshold ) addAlignCell(prop1,prop2, "=", val);
		}
	    }
	    // Extract for classes
	    for ( Object class1 : getOntologyObject1().getClasses() ) {
		for ( Object class2 : getOntologyObject2().getClasses() ) {
		    if ( sim.getSimilarity() ) val = sim.getClassSimilarity(class1,class2);
		    else val = 1. - sim.getClassSimilarity(class1,class2);
		    if (val > threshold ) addAlignCell(class1, class2, "=", val);
		}
	    }
	    // Extract for individuals
	    if (  params.getProperty("noinst") == null ){
		for ( Object ind1 : getOntologyObject1().getIndividuals() ) {
		    if ( getOntologyObject1().getEntityURI( ind1 ) != null ) {
			for ( Object ind2 : getOntologyObject2().getIndividuals() ) {
			    if ( getOntologyObject2().getEntityURI( ind2 ) != null ) {
				if ( sim.getSimilarity() ) val = sim.getIndividualSimilarity( ind1, ind2 );
				else val = 1 - sim.getIndividualSimilarity( ind1, ind2 );
				if ( val > threshold ) addAlignCell(ind1,ind2, "=", val);
			    }
			}
		    }
		}
	    }
	} catch (OntowrapException owex) { 
	    logger.debug( "IGNORED Exception", owex );
	} catch (AlignmentException alex) { 
	    logger.debug( "IGNORED Exception", alex );
	}
	return((Alignment)this);
    }

    /**
     * Extract the alignment of a ?? type
     * 
     * exact algorithm using the Hungarian method.
     * This algorithm contains several guards to prevent the HungarianAlgorithm to
     * raise problems:
     * - It invert column and rows when nbrows &gt; nbcol (Hungarian loops)
     * - It prevents to generate alignments when one category has no elements.
     * 
     * @param threshold: the threshold to apply to extracted correspondences
     * @param params: the parameter of the measure
     * @return the extracted alignment
     */
    @SuppressWarnings({"unchecked","rawTypes"}) //ConcatenatedIterator
    public Alignment extractqq( double threshold, Properties params) {
	try {
	    // A STRAIGHTFORWARD IMPLEMENTATION
	    // (redoing the matrix instead of getting it)
	    // For each kind of stuff (cl, pr, ind)
	    // Create a matrix
	    int nbclasses1 = getOntologyObject1().nbClasses();
	    int nbclasses2 = getOntologyObject2().nbClasses();
	    if ( nbclasses1 != 0 && nbclasses2 != 0 ) {
		double[][] matrix = new double[nbclasses1][nbclasses2];
		Object[] class1 = new Object[nbclasses1];
		Object[] class2 = new Object[nbclasses2];
		int i = 0;
		for ( Object ob : getOntologyObject1().getClasses() ) {
		    class1[i++] = ob;
		}
		int j = 0;
		for ( Object ob : getOntologyObject2().getClasses() ) {
		    class2[j++] = ob;
		}
		for( i = 0; i < nbclasses1; i++ ){
		    for( j = 0; j < nbclasses2; j++ ){
			if ( sim.getSimilarity() ) matrix[i][j] = sim.getClassSimilarity(class1[i],class2[j]);
			else matrix[i][j] = 1. - sim.getClassSimilarity(class1[i],class2[j]);
		    }
		}
		// Pass it to the algorithm
		int[][] result = callHungarianMethod( matrix, nbclasses1, nbclasses2 );
		// Extract the result
		for( i=0; i < result.length ; i++ ){
		    // The matrix has been destroyed
		    double val;
		    if ( sim.getSimilarity() ) val = sim.getClassSimilarity(class1[result[i][0]],class2[result[i][1]]);
		    else val = 1 - sim.getClassSimilarity(class1[result[i][0]],class2[result[i][1]]);
		    // JE: here using strict-> is a very good idea.
		    // it means that correspondences with 0. similarity
		    // will be excluded from the best match. 
		    if( val > threshold ){
			addAlignCell( (String)null, class1[result[i][0]], class2[result[i][1]], "=", val );
		    }
		}
	    }
	} catch (OntowrapException owex) { 
	    logger.debug( "IGNORED Exception", owex );
	} catch (AlignmentException alex) { 
	    logger.debug( "IGNORED Exception", alex );
	}
	// For properties
	try{
	    int nbprop1 = getOntologyObject1().nbProperties();
	    int nbprop2 = getOntologyObject2().nbProperties();
	    if ( nbprop1 != 0 && nbprop2 != 0 ) {
		double[][] matrix = new double[nbprop1][nbprop2];
		Object[] prop1 = new Object[nbprop1];
		Object[] prop2 = new Object[nbprop2];
		int i = 0;
		ConcatenatedIterator<Object> pit1 = new 
		    ConcatenatedIterator<Object>(getOntologyObject1().getObjectProperties().iterator(),
					 getOntologyObject1().getDataProperties().iterator());
		for ( Object ob: pit1 ) prop1[i++] = ob;
		int j = 0;
		ConcatenatedIterator<Object> pit2 = new 
		    ConcatenatedIterator<Object>(getOntologyObject2().getObjectProperties().iterator(),
					 getOntologyObject2().getDataProperties().iterator());
		for ( Object ob: pit2 ) prop2[j++] = ob;
		for( i = 0; i < nbprop1; i++ ){
		    for( j = 0; j < nbprop2; j++ ){
			if ( sim.getSimilarity() ) matrix[i][j] = sim.getPropertySimilarity(prop1[i],prop2[j]);
			else matrix[i][j] = 1. - sim.getPropertySimilarity(prop1[i],prop2[j]);
		    }
		}
		// Pass it to the algorithm
		int[][] result = callHungarianMethod( matrix, nbprop1, nbprop2 );
		// Extract the result
		for( i=0; i < result.length ; i++ ){
		    // The matrix has been destroyed
		    double val;
		    if ( sim.getSimilarity() ) val = sim.getPropertySimilarity(prop1[result[i][0]],prop2[result[i][1]]);
		    else val = 1 - sim.getPropertySimilarity(prop1[result[i][0]],prop2[result[i][1]]);
		    // JE: here using strict-> is a very good idea.
		    // it means that alignments with 0. similarity
		    // will be excluded from the best match. 
		    if( val > threshold ){
			addAlignCell( (String)null, prop1[result[i][0]], prop2[result[i][1]], "=", val );
		    }
		}
	    }
	} catch (OntowrapException owex) { 
	    logger.debug( "IGNORED Exception", owex );
	} catch (AlignmentException alex) { 
	    logger.debug( "IGNORED Exception", alex );
	}
	// For individuals
	if (  params.getProperty("noinst") == null ){
	    try {
		// Create individual lists
		Object[] ind1 = new Object[getOntologyObject1().nbIndividuals()];
		Object[] ind2 = new Object[getOntologyObject2().nbIndividuals()];
		int nbind1 = 0;
		int nbind2 = 0;
		for( Object ob : getOntologyObject2().getIndividuals() ){
		    // We suppress anonymous individuals... this is not legitimate
		    if ( getOntologyObject2().getEntityURI( ob ) != null ) {
			ind2[nbind2++] = ob;
		    }
		}
		for( Object ob : getOntologyObject1().getIndividuals() ){
		    // We suppress anonymous individuals... this is not legitimate
		    if ( getOntologyObject1().getEntityURI( ob ) != null ) {
			ind1[nbind1++] = ob;
		    }
		}
		if ( nbind1 != 0 && nbind2 != 0 ) {
		    double[][] matrix = new double[nbind1][nbind2];
		    int i, j;
		    for( i=0; i < nbind1; i++ ){
			for( j=0; j < nbind2; j++ ){
			    if ( sim.getSimilarity() ) matrix[i][j] = sim.getIndividualSimilarity(ind1[i],ind2[j]);
			    else matrix[i][j] = 1 - sim.getIndividualSimilarity(ind1[i],ind2[j]);
			}
		    }
		    // Pass it to the algorithm
		    int[][] result = callHungarianMethod( matrix, nbind1, nbind2 );
		    // Extract the result
		    for( i=0; i < result.length ; i++ ){
			// The matrix has been destroyed
			double val;
			if ( sim.getSimilarity() ) val = sim.getIndividualSimilarity(ind1[result[i][0]],ind2[result[i][1]]);
			else val = 1 - sim.getIndividualSimilarity(ind1[result[i][0]],ind2[result[i][1]]);
			// JE: here using strict-> is a very good idea.
			// it means that alignments with 0. similarity
			// will be excluded from the best match. 
			if( val > threshold ){
			    addAlignCell( (String)null, ind1[result[i][0]], ind2[result[i][1]], "=", val );
			}
		    }
		}
	    } catch (OntowrapException owex) { 
		logger.debug( "IGNORED Exception", owex );
	    } catch (AlignmentException alex) { 
		logger.debug( "IGNORED Exception", alex );
	    }
	}
	return((Alignment)this);
    }

    public int[][] callHungarianMethod( double[][] matrix, int i, int j ) {
	boolean transposed = false;
	if ( i > j ) { // transposed aray (because rows>columns).
	    matrix = HungarianAlgorithm.transpose(matrix);
	    transposed = true;
	}
	int[][] result = HungarianAlgorithm.hgAlgorithm( matrix, "max" );
	if ( transposed ) {
	    for( int k=0; k < result.length ; k++ ) { 
		int val = result[k][0]; result[k][0] = result[k][1]; result[k][1] = val; 
	    }
	    
	}
	return result;
    }

    /**
     * Greedy algorithm:
     * 1) dump the part of the matrix distance above threshold in a sorted set
     * 2) traverse the sorted set and each time a correspondence involving two
     *    entities that have no correspondence is encountered, add it to the 
     *    alignment.
     * Complexity: O(n^2.logn)
     * Pitfall: no global optimality is warranted, nor stable marriage
     * for instance if there is the following matrix:
     * (a,a')=1., (a,b')=.9, (b,a')=.9, (b,b')=.1
     * This algorithm will select the first and last correspondances of
     * overall similarity 1.1, while the optimum is the second solution
     * with overall of 1.8.
     * 
     * @param threshold: the threshold to apply to extracted correspondences
     * @param params: the parameter of the measure
     * @return the extracted alignment
     */
    @SuppressWarnings({"unchecked","rawTypes"}) //ConcatenatedIterator
    public Alignment extractqqgreedy( double threshold, Properties params) {
	double val = 0;
	//TreeSet could be replaced by something else
	//The comparator must always tell that things are different!
	SortedSet<CellStruct> cellSet = new TreeSet<CellStruct>(
			    new Comparator<CellStruct>() {
				public int compare( CellStruct o1, CellStruct o2 )
				    throws ClassCastException{
				    try {
					//logger.trace("{} -- {} // {} -- {}", o1.getObject1(), o1.getObject2(), o2.getObject1(), o2.getObject2());
					if ( o1.distance > o2.distance ){
					    return -1;
					} else if ( o1.distance < o2.distance ){
					    return 1;
					} else if ( getOntologyObject1().getEntityName( o1.object1 ) == null
						    || getOntologyObject2().getEntityName( o2.object1 ) == null ) {
					    return -1;
					} else if ( getOntologyObject1().getEntityName( o1.object1).compareTo( getOntologyObject2().getEntityName( o2.object1 ) ) > 0 ) {
					    return -1;
					} else if ( getOntologyObject1().getEntityName( o1.object1).compareTo( getOntologyObject2().getEntityName( o2.object1 ) ) < 0 ) {
					    return 1;
					} else if ( getOntologyObject1().getEntityName( o1.object2 ) == null
						    || getOntologyObject2().getEntityName( o2.object2 ) == null ) {
					    return -1;
					} else if ( getOntologyObject1().getEntityName( o1.object2).compareTo( getOntologyObject2().getEntityName( o2.object2 ) ) > 0 ) {
					    return -1;
					// Assume they have different names
					} else { return 1; }
				    } catch ( OntowrapException e) { 
					e.printStackTrace(); return 0;}
				}
			    }
			    );
      try {
	  // Get all the matrix above threshold in the SortedSet
	  // Plus a map from the objects to the cells
	  // O(n^2.log n)
	  // for classes
	  for ( Object ent1: getOntologyObject1().getClasses() ) {
	      for ( Object ent2: getOntologyObject2().getClasses() ) {
		  if ( sim.getSimilarity() ) val = sim.getClassSimilarity( ent1, ent2 );
		  else val = 1 - sim.getClassSimilarity( ent1, ent2 );
		  if ( val > threshold ){
		      cellSet.add( new CellStruct( ent1, ent2, val ) );
		  }
	      }
	  }
	  // for properties
	  ConcatenatedIterator<Object> pit1 = new 
	      ConcatenatedIterator<Object>(getOntologyObject1().getObjectProperties().iterator(),
				   getOntologyObject1().getDataProperties().iterator());
	  for ( Object ent1: pit1 ) {
	      ConcatenatedIterator<Object> pit2 = new 
		  ConcatenatedIterator<Object>(getOntologyObject2().getObjectProperties().iterator(),
					getOntologyObject2().getDataProperties().iterator());
	      for ( Object ent2: pit2 ) {
		  if ( sim.getSimilarity() ) val = sim.getPropertySimilarity( ent1, ent2 );
		  else val = 1 - sim.getPropertySimilarity( ent1, ent2 );
		  if ( val > threshold ){
		      cellSet.add( new CellStruct( ent1, ent2, val ) );
		  }
	      }
	  }
	  // for individuals
	  if (  params.getProperty("noinst") == null ){
	      for( Object ent1: getOntologyObject1().getIndividuals() ) {
		  if ( getOntologyObject1().getEntityURI( ent1 ) != null ) {

		      for( Object ent2: getOntologyObject2().getIndividuals() ) {
			  if ( getOntologyObject2().getEntityURI( ent2 ) != null ) {
			      if ( sim.getSimilarity() ) val = sim.getIndividualSimilarity( ent1, ent2 );
			      else val = 1 - sim.getIndividualSimilarity( ent1, ent2 );
			      if ( val > threshold ){
				  cellSet.add( new CellStruct( ent1, ent2, val ) );
			      }
			  }
		      }
		  }
	      }
	  }

	  // O(n^2)
	  for( CellStruct cell : cellSet ){
	      Object ent1 = cell.object1;
	      Object ent2 = cell.object2;
	      if ( (getAlignCells1( ent1 ) == null) && (getAlignCells2( ent2 ) == null) ){
		  // The cell is directly added!
		  addAlignCell( (String)null, ent1, ent2, "=", cell.distance );
	      }
	  };

      } catch (OntowrapException owex) { 
	  logger.debug( "IGNORED Exception", owex );
      } catch (AlignmentException alex) { 
	  logger.debug( "IGNORED Exception", alex );
      }
      return((Alignment)this);
    }

}

class CellStruct {

    public Object object1;
    public Object object2;
    public double distance;

    public CellStruct( Object entity1, Object entity2, double val ) {
	object1 = entity1;
	object2 = entity2;
	distance = val;
    }

}
