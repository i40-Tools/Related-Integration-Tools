/*
 * $Id: AlignmentProcess.java 1598 2011-05-14 20:58:47Z euzenat $
 *
 * Copyright (C) INRIA, 2003-2004, 2007, 2009, 2011
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

/**
 * Represents an executable alignment
 *
 * @author J�r�me Euzenat
 * @version $Id: AlignmentProcess.java 1598 2011-05-14 20:58:47Z euzenat $ 
 */

public interface AlignmentProcess extends Alignment
{
    /** Processing **/
    /**
     * The align method computes the alignment from the ontologies.
     * It is hightly dependent on the implementation.
     * It take an initial Alignment as input.
     */
    public void align( Alignment alignment, Properties param ) throws AlignmentException;

    /**
     * Returns the parameters that the align() method is able to process.
     * In spirit, this method does not need an instance so it should be
     * implemented as a static. But this is not supported in interface
     * declarations (it should!).
     * This parameters set can be null if no parameters are interpreted or
     * contains the list of entries accepted. These entries can be set and
     * the Parameters passed to the align() method.
     *
     * public Properties getParameters();
     */

}

