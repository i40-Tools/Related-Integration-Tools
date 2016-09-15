/*
 * $Id: RenderedNetwork.java 1967 2014-09-06 15:45:57Z euzenat $
 *
 * Copyright (C) INRIA, 2014
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

public class RenderedNetwork extends Success {

    public RenderedNetwork ( int surr, Message rep, String from, String to, String cont, Properties param ) {
	super( surr, rep, from, to, cont, param );
    }

    public RenderedNetwork ( Properties mess, int surr, String from, String cont ) {
	super( mess, surr, from, cont );
	parameters = mess; // This is used below
    }

    public String RESTString(){ // At the moment, this is only XML
	//String method = (parameters==null)?null:parameters.getProperty( "method" );
	//if ( method != null && method.equals("fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor" ) ) {
	    return "<network>"+content+"</network>";
	    //} else {
	    //return "<network>"+getXMLContent()+"</network>";
	    //}
    }
    public String JSONString(){
	//String method = (parameters==null)?null:parameters.getProperty( "method" );
	//if ( method != null && method.equals("fr.inrialpes.exmo.align.impl.renderer.JSONRendererVisitor" ) ) {
	//return content;
	//} else {
	    return "\""+getJSONContent()+"\"";
	    //}
    }
}
