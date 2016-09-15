/*
 * $Id: AlgebraRelation.java 2082 2015-10-21 08:07:45Z euzenat $
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

/**
 * Represents a relation from an algebra of relations
 * 
 * In addition to meet and join, it provides compose and inverse
 *
 * @author Jérôme Euzenat
 * @version $Id: AlgebraRelation.java 2082 2015-10-21 08:07:45Z euzenat $
 */

public interface AlgebraRelation<T extends BaseRelation> extends DisjunctiveRelation<T> {

    public AlgebraRelation<T> compose( AlgebraRelation<T> dr ) throws AlignmentException;

    public AlgebraRelation<T> inverse();

    /**
     * They should be part of the interface...
     * However, they are static
     * Hence static properties are not available in interfaces...

    public AlgebraRelation<T> getIdRelation();

    public AlgebraRelation<T> getNoInfoRelation();

    public AlgebraRelation<T> getInconsistentRelation();

     */
}

