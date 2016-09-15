/*
 * $Id: AverageAggregator.java 2033 2015-02-07 17:24:09Z euzenat $
 *
 * Copyright (C) INRIA, 2010, 2013, 2015
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

package fr.inrialpes.exmo.align.impl.aggr; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;

/**
 *
 * @author Jérôme Euzenat
 * @version $Id: AverageAggregator.java 2033 2015-02-07 17:24:09Z euzenat $ 
 *
 * Aggregate alignments by recording the proportin of "vote" for each correspondence
 */

public class AverageAggregator extends BasicAggregator {
    final static Logger logger = LoggerFactory.getLogger( AverageAggregator.class );

    /** Creation **/
    public AverageAggregator() {
	super();
	setExtension( Namespace.EXT.uri, Annotations.METHOD, "fr.inrialpes.exmo.align.impl.aggr.AverageAggregator" );
    }

    /**
     * Set the confidence of correspondence in the alignments as the average of confidence
     * that they obtained in the aggregated alignments (confidence is 0. if the correspondence
     * was not there).
     */
    public void extract() {
	for ( Cell c : this ) {
	    c.setStrength( count.get( c ).getValue() / (double)nbAlignments );
	}
    }

}
