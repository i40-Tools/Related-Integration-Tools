/*
 * $Id: OntologyNetworkWeakener.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2009-2011
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

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.OntologyNetwork;

import java.util.Properties;

/**
 * OntologyNetworkWeakener
 *
 * A interface which alters ontology networks.
 * NOTE: These two primitives would benefit from the hability to declare static in interfaces...
 * Now we have to create instances of the implementing classes.
 *
 */
public interface OntologyNetworkWeakener {

    /**
     * Weakens the ontology network of n units
     *
     * @param on: the network of ontologies to weaken
     * @param n: the extent of the weakening in units
     * @param p: weakening parameters
     * @throws AlignmentException whenever something goes wrong
     * @return the weakened network
     */
    public OntologyNetwork weaken( OntologyNetwork on, int n, Properties p ) throws AlignmentException;

    /**
     * Weakens the ontology network of n%
     *
     * @param on: the network of ontologies to weaken
     * @param n: the extent of the weakening in percentage
     * @param p: weakening parameters
     * @throws AlignmentException whenever something goes wrong
     * @return the weakened network
     */
    public OntologyNetwork weaken( OntologyNetwork on, double n, Properties p ) throws AlignmentException;

}
