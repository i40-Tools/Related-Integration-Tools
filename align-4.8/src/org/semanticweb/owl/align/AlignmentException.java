/*
 * $Id: AlignmentException.java 1866 2014-01-14 14:34:59Z euzenat $
 *
 * Copyright (C) INRIA Rh�ne-Alpes, 2003-2004, 2008
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

import java.lang.Exception;

/**
 * Base class for all Alignment Exceptions.
 *
 *
 * @author J�r�me Euzenat
 * @version $Id: AlignmentException.java 1866 2014-01-14 14:34:59Z euzenat $
 */

public class AlignmentException extends Exception {

    private static final long serialVersionUID = 330;

    public AlignmentException( String message ) {
	super( message );
    }
    
    public AlignmentException( String message, Exception ex ) {
	super( message, ex );
    }
    
}

