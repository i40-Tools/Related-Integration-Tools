/*
 * $Id: Alterator.java 2157 2017-07-19 10:15:36Z euzenat $
 *
 * Copyright (C) INRIA, 2011, 2015, 2017
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

package fr.inrialpes.exmo.align.gen;

import java.util.Properties;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;

import org.apache.jena.ontology.OntModel;

/**
 * An abstract test generator which takes as input an ontology and an
 * alignment between this ontology and another one and transform the
 * ontology and the alignment accordingly to a type of alteration.
 *
 * It follows a particular lifecycle
 */
public interface Alterator {

    /**
     * It is created either:
     * - from a seed ontology and generate the alignment between this
     *   ontology itself
     * - from a previous alterator from which it will take the output
     *   ontology and alignment as input.
     */
    //public Alterator( Alterator om );

    /**
     * @return the namespace of the input ontology
     */
    public String getNamespace();
    /**
     * @return the namespace of the source ontology in the input alignment
     */
    public String getBase();

    /**
     * modify applies the alteration to the input (the results are kept in 
     * internal structures.
     *
     * @param params: the alteration to be performed
     * @return the Alterator object with alterations performed
     * @throws AlignmentException when the modification cannot happen
     */
    public Alterator modify( Properties params ) throws AlignmentException;

    // Temporary
    /**
     * getProtoAlignment, getProtoOntology, getHierarchy
     * are used for accessing these internal structure at creation time.
     *
     * @return properties representing a currect alignment
     */
    public Properties getProtoAlignment();
    public OntModel getProtoOntology();
    public ClassHierarchy getHierarchy();

    /**
     * Modifies the namespaces of source and target ontologies
     * (for the main purpose of outputing them)
     * 
     * @param namespace1 and
     * @param namespace2: the namespaces to be given to source and target ontologies
     */
    public void relocateTest( String namespace1, String namespace2 );
    public void relocateTest( String namespace2 );

    /**
     * @return the altered Alignment in output form
     */
    public Alignment getAlignment();
    /**
     * @return the altered Ontology in output form
     */
    public OntModel getModifiedOntology();

}
