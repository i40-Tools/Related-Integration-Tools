/*
 * $Id: RenderedAlignment.java 1967 2014-09-06 15:45:57Z euzenat $
 *
 * Copyright (C) INRIA, 2006, 2008-2009, 2011, 2014
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package fr.inrialpes.exmo.align.service.msg;

import java.util.Properties;

/**
 * Contains the messages that should be sent according to the protocol
 */

public class RenderedAlignment extends Success {

    public RenderedAlignment ( int surr, Message rep, String from, String to, String cont, Properties param ) {
	super( surr, rep, from, to, cont, param );
    }

    public RenderedAlignment ( Properties mess, int surr, String from, String cont ) {
	super( mess, surr, from, cont );
	parameters = mess; // This is used below
    }

    public String RESTString(){
	String method = (parameters==null)?null:parameters.getProperty( "method" );
	if ( method != null
	     && ( method.equals("fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor" )
		  || method.equals("fr.inrialpes.exmo.align.impl.renderer.COWLMappingRendererVisitor" )
		  || method.equals("fr.inrialpes.exmo.align.impl.renderer.SWRLRendererVisitor" )
		  || method.equals("fr.inrialpes.exmo.align.impl.renderer.XSLTRendererVisitor" )
		  || method.equals("fr.inrialpes.exmo.align.impl.renderer.OWLAxiomsRendererVisitor" )
		  || method.equals("fr.inrialpes.exmo.align.impl.renderer.XMLMetadataRendererVisitor" )
		  || method.equals("fr.inrialpes.exmo.align.impl.renderer.SKOSRendererVisitor" ) ) ) {
	    return "<alignment>"+content+"</alignment>";
	} else {
	    return "<alignment>"+getXMLContent()+"</alignment>";
	}
    }
    public String JSONString(){
	String method = (parameters==null)?null:parameters.getProperty( "method" );
	if ( method != null && method.equals("fr.inrialpes.exmo.align.impl.renderer.JSONRendererVisitor" ) ) {
	    return content;
	} else {
	    return "\""+getJSONContent()+"\"";
	}
    }
}
