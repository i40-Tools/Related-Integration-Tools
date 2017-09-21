/*
 * $Id: NameEqAlignment.java 1256 2010-02-15 22:27:54Z euzenat $
 *
 * Copyright (C) INRIA Rh�ne-Alpes, 2003-2005, 2007-2008, 2010
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

package fr.inrialpes.exmo.align.impl.method; 

import org.semanticweb.owl.align.AlignmentProcess;

/**
 * Matches two oontologies based on the equality of the name of their entities.
 * THIS CLASS IS ONLY HERE FOR COMPATIBILITY PURPOSES
 *
 * @author J�r�me Euzenat
 * @version $Id: NameEqAlignment.java 1256 2010-02-15 22:27:54Z euzenat $ 
 */

public class NameEqAlignment extends StringDistAlignment implements AlignmentProcess {
	
    /** Creation **/
    public NameEqAlignment(){
	methodName = "equalDistance";
    };

}
