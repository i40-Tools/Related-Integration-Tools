/*
 * $Id: OntologyNetworkId.java 1905 2014-03-21 09:12:16Z euzenat $                                                                                                                         
 *
 * Copyright (C) INRIA, 2006-2009, 2011, 2013-2014
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

public class OntologyNetworkId extends Success {

    String pretty = null;

    public OntologyNetworkId ( int surr, Message rep, String from, String to, String cont, Properties param ) {
	super( surr, rep, from, to, cont, param );
    }

    public OntologyNetworkId ( int surr, Message rep, String from, String to, String cont, Properties param, String pretty ) {
	super( surr, rep, from, to, cont, param );
	this.pretty = pretty;
    }

    public OntologyNetworkId ( Properties mess, int surr, String from, String cont, String pretty) {
	super( mess, surr, from, cont );
	this.pretty = pretty;
    }

    public OntologyNetworkId ( Properties mess, int surr, String from, String cont ) {
	super( mess, surr, from, cont );
    }

    public String getPretty( String ontonet ) {
	if ( pretty == null ) {
	    return ontonet;                                                                                                                                                                 
	} else {
	    return ontonet+" ("+pretty+")";                                                                                                                                                 
	}
    }

    public String HTMLString(){
	//return "Ontology Network ID: <a href=\"../ontonet/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor&id="+getContent()+"\">"+getPretty(getContent())+"</a>";
	return "Ontology Network ID: <a href=\""+getContent()+"\">"+getPretty(getContent())+"</a>";
    }

    public String HTMLRESTString(){
	return "Ontology Network ID: <a href=\"../rest/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor&id="+getContent()+"\">"+getPretty(getContent())+"</a>";
    }

    public String RESTString(){
	return "<alid>"+content+"</alid>";
    }

    public String JSONString(){
	return "\""+content+"\"";
    }
}
