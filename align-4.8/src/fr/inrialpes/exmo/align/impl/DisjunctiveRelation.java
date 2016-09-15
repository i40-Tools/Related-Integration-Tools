/*
 * $Id: DisjunctiveRelation.java 2083 2015-10-21 10:14:19Z euzenat $
 *
 * Copyright (C) INRIA, 2015
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

import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Relation;

import java.lang.Iterable;

/**
 * Represents a dijunctive ontology alignment relation.
 * A disjunctive ontology alignment relation is made of the disjunction of several atomic relations.
 * This disjunction is traditionally represented as a set.
 * This allows for the natural implementation of the conjunction and disjunction operation.
 *
 * @author Jérôme Euzenat
 * @version $Id: DisjunctiveRelation.java 2083 2015-10-21 10:14:19Z euzenat $
 */

public interface DisjunctiveRelation<T extends BaseRelation> extends Relation, Iterable<T> {

    public T getRelation( String rel );

    @SuppressWarnings({"unchecked", "varargs"}) public DisjunctiveRelation<T> join( DisjunctiveRelation<T>... drs ) throws AlignmentException;

    @SuppressWarnings({"unchecked", "varargs"}) public DisjunctiveRelation<T> meet( DisjunctiveRelation<T>... drs ) throws AlignmentException;

    public boolean isEmpty();

    //public boolean entails( DisjunctiveRelation<T> dr );

}

