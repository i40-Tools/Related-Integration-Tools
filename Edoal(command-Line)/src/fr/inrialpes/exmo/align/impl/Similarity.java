/*
 * $Id: Similarity.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2004, 2006-2010
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

import org.semanticweb.owl.align.Alignment;

import java.util.Properties;

import fr.inrialpes.exmo.ontowrap.LoadedOntology;

/**
 * Represents the implementation of a similarity measure
 *
 * @author Jérôme Euzenat
 * @version $Id: Similarity.java 2102 2015-11-29 10:29:56Z euzenat $ 
 */

public interface Similarity
{
    // These parameters contains usually:
    // ontology1 and ontology2
    /**
     * Is it a similarity or a distance?
     *
     * @return true if the object represent a similarity (otherwise, it is a distance)
     */
    public boolean getSimilarity();

    // JE: OntoRewr: This should not be in init
    /**
     * Initialize the similarity value with various useful structures
     * 
     * @param onto1 and
     * @param onto2: the two ontologies which are matched
     */
    public void initialize( LoadedOntology<Object> onto1, LoadedOntology<Object> onto2 );
    public void initialize( LoadedOntology<Object> onto1, LoadedOntology<Object> onto2, Alignment align );

    /**
     * actually computes the similarity and store it in the adequate structures
     * 
     * @param p: the parameters of the measure to compute
     */
    public void compute( Properties p );

    /**
     * Accessors to the stored similarity values
     * 
     * @param c1 and
     * @param c2: the two objects for which computing the similarity
     * @return the measure value
     */
    public double getClassSimilarity( Object c1, Object c2 );
    public double getPropertySimilarity( Object p1, Object p2);
    public double getIndividualSimilarity( Object i1, Object i2 );

    /**
     * Printers of the obtained similarity values
     *
     * @param type: the type of layout of the printout
     */
    public void printClassSimilarityMatrix( String type );
    public void printPropertySimilarityMatrix( String type );
    public void printIndividualSimilarityMatrix( String type );

    // JE2010: These are used by the generic MatrixMeasure implementation
    // in which it is sufficient to implement them to solve everything
    // However, for more flexibility, it possible to just skip these
    // and implement the measure within compute(p) (see InstanceBasedMatrixMeasure).
    public double classMeasure( Object c1, Object c2 ) throws Exception;
    public double propertyMeasure( Object p1, Object p2) throws Exception;
    public double individualMeasure( Object i1, Object i2 ) throws Exception;
}

