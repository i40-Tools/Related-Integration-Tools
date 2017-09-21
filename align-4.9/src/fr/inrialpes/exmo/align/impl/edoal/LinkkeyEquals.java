/*
 * $Id: LinkkeyEquals.java 2101 2015-11-20 23:16:46Z euzenat $
 *
 * Copyright (C) INRIA, 2015
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

package fr.inrialpes.exmo.align.impl.edoal;

import org.semanticweb.owl.align.AlignmentException;

/**
 *
 * @author Nicolas Guillouet (nicolas@meaningengines.com)
 */
public class LinkkeyEquals extends LinkkeyBinding{
    
    public LinkkeyEquals( PathExpression expression1, PathExpression expression2 ) {
        super( expression1, expression2 );
    }
    
    public void accept( EDOALVisitor visitor ) throws AlignmentException {
	visitor.visit( this );
    }

    public LinkkeyEquals inverse() {
	return new LinkkeyEquals( expression2, expression1 );
    }
}
