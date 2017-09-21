/*
 * $Id: AlignmentRepairer.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2014
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

package org.semanticweb.owl.align; 

import java.util.Properties;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.OntologyNetwork;


/**
 * Represents an operator for repairing alignments and networks of alignments.
 *
 * @author Jérôme Euzenat
 * @version $Id: AlignmentRepairer.java 2102 2015-11-29 10:29:56Z euzenat $ 
 */

public interface AlignmentRepairer {

    /**
     * Has to be invoked before using repair.
     * May be used for initialising global structures.
     *
     * Known parameters (for both alignments and networks):
     * - reasoner (hermit|pellet|elk): which reasoner is used
     *
     * @param param: set of properties expressing the parameters for this repairer
     * @throws AlignmentException when something goes wrong
     */
    public void init( Properties param ) throws AlignmentException;

    /** Repairing **/

    /**
     * Perform repair on the alignment.
     * The ontology may be obtained from the values of getOntology1() and getOntology2() 
     * These are objects that may be URI, instance of Ontowrap or
     * instances of OWLOntologies.
     *
     * Known parameters (for both alignments and networks):
     * - postSATCheck (boolean): does the repairer performs a satisfiability check after repair
     * - extractModules (boolean): does the repairer first extract modules
     * - completeness (complete|efficient|bruteforce): is the reasoning complete or approximate
     * - optimality (global|local|greedy|greedymin|globaliso): does the repairer look for particular optimum
     * - entities (concepts|properties|conceptproperties): types of correspondences taken into account
     * -
     *
     * @param alignment: the alignment to be repaired
     * @param param: specific repairing parameters as properties
     * @return the repaired alignment
     * @throws AlignmentException when something goes wrong
     */
    public Alignment repair( Alignment alignment, Properties param ) throws AlignmentException;

    /**
     * Perform repair on a whole network of ontologies.
     *
     * @param network: the network of ontologies to be repaired
     * @param param: specific repairing parameters as properties
     * @return the repaired network of ontologies
     * @throws AlignmentException when something goes wrong
     */
    public OntologyNetwork repair( OntologyNetwork network, Properties param ) throws AlignmentException;

}

