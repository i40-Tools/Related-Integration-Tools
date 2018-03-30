/*
 * $Id: XMLMetadataRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $
 *
 * Copyright (C) INRIA, 2007, 2009-2010, 2012, 2014-2016
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

package fr.inrialpes.exmo.align.impl.renderer; 

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;

import fr.inrialpes.exmo.align.impl.Annotations;
import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.BasicAlignment;

/**
 * Renders an alignment in its RDF format
 *
 * @author Jérôme Euzenat
 * @version $Id: XMLMetadataRendererVisitor.java 2116 2016-09-19 08:38:32Z euzenat $ 
 */

public class XMLMetadataRendererVisitor extends IndentedRendererVisitor implements AlignmentVisitor {
    final static Logger logger = LoggerFactory.getLogger(XMLMetadataRendererVisitor.class);
    
    Alignment alignment = null;
    boolean embedded = false; // if the output is XML embeded in a structure
    Hashtable<String,String> nslist = null;
    boolean newstyle = false;

    public XMLMetadataRendererVisitor( PrintWriter writer ){
	super( writer );
    }

    public void init( Properties p ) {
	super.init( p );
	if ( p.getProperty( "embedded" ) != null 
	     && !p.getProperty( "embedded" ).equals("") ) embedded = true;
	if ( p.getProperty( "newstyle" ) != null 
	     && !p.getProperty( "newstyle" ).equals("") ) newstyle = true;
    };

    public void visit( Alignment align ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, align, Alignment.class ) ) return;
	// default behaviour
	String extensionString = "";
	alignment = align;
	nslist = new Hashtable<String,String>();
        nslist.put(Namespace.ALIGNMENT.prefix, Namespace.ALIGNMENT.shortCut);
        nslist.put(Namespace.EXT.prefix, Namespace.EXT.shortCut);
        nslist.put(Namespace.RDF.prefix, Namespace.RDF.shortCut);
        nslist.put(Namespace.XSD.prefix, Namespace.XSD.shortCut);
	// Get the keys of the parameter
	int gen = 0;
	for ( String[] ext : align.getExtensions() ){
	    String prefix = ext[0];
	    String name = ext[1];
	    String tag = nslist.get(prefix);
	    if ( tag == null ) {
		tag = "ns"+gen++;
		nslist.put( prefix, tag );
	    }
	    if ( tag.equals("align") ) { tag = name; }
	    else { tag += ":"+name; }
	    extensionString += INDENT+INDENT+"<"+tag+">"+ext[2]+"</"+tag+">"+NL;
	}
	if ( embedded == false ) {
	    indentedOutputln("<?xml version='1.0' encoding='"+ENC+"' standalone='no'?>");
	}
	writer.print("<rdf:RDF xmlns='"+Namespace.ALIGNMENT.uri+"'");
	for ( Entry<String,String> e : nslist.entrySet() ) {
	    writer.print("\n         xmlns:"+e.getValue()+"='"+e.getKey()+"'");
	}
	if ( align instanceof BasicAlignment ) {
	    for ( Entry<Object,Object> e : ((BasicAlignment)align).getXNamespaces().entrySet() ) {
		String label = (String)e.getKey();
		if ( !label.equals("rdf") && !label.equals("xsd")
		     && !label.equals("<default>") )
		    writer.print("\n         xmlns:"+label+"='"+e.getValue()+"'");
	    }
	}
	indentedOutputln(">");
	increaseIndent();
	indentedOutput("<Alignment");
	String idext = align.getExtension( Namespace.ALIGNMENT.uri, Annotations.ID );
	if ( idext != null ) {
	    output(" rdf:about=\""+idext+"\"");
	}
	outputln(">");
	increaseIndent();
	indentedOutputln("<xml>yes</xml>");
	indentedOutputln("<level>"+align.getLevel()+"</level>");
	indentedOutputln("<type>"+align.getType()+"</type>");
	// Get the keys of the parameter
	if ( !newstyle ) {
	    if ( align.getFile1() != null )
		indentedOutputln("<onto1>"+align.getFile1().toString()+"</onto1>");
	    if ( align.getFile2() != null )
		indentedOutputln("<onto2>"+align.getFile2().toString()+"</onto2>");
	    indentedOutputln("<uri1>"+align.getOntology1URI()+"</uri1>");
	    indentedOutputln("<uri2>"+align.getOntology2URI()+"</uri2>");
	    output(extensionString);
	} else {
	    indentedOutputln("<onto1>");
	    increaseIndent();
	    indentedOutput("<Ontology");
	    if ( align.getOntology1URI() != null ) {
		output(" rdf:about=\""+align.getOntology1URI()+"\"");
	    }
	    outputln(">");
	    increaseIndent();
	    indentedOutputln("<location>"+align.getFile1()+"</location>");
	    if ( align instanceof BasicAlignment && ((BasicAlignment)align).getOntologyObject1().getFormalism() != null ) {
		indentedOutputln("<formalism>");
		increaseIndent();
		indentedOutputln("<Formalism align:name=\""+((BasicAlignment)align).getOntologyObject1().getFormalism()+"\" align:uri=\""+((BasicAlignment)align).getOntologyObject1().getFormURI()+"\"/>");
		decreaseIndent();
		indentedOutputln("</formalism>");
	    }
	    decreaseIndent();
	    indentedOutputln("</Ontology>");
	    decreaseIndent();
	    indentedOutputln("</onto1>");
	    indentedOutputln("<onto2>");
	    increaseIndent();
	    indentedOutput("<Ontology");
	    if ( align.getOntology2URI() != null ) {
		output(" rdf:about=\""+align.getOntology2URI()+"\"");
	    }
	    outputln(">");
	    increaseIndent();
	    indentedOutputln("<location>"+align.getFile2()+"</location>");
	    if ( align instanceof BasicAlignment && ((BasicAlignment)align).getOntologyObject2().getFormalism() != null ) {
		indentedOutputln("<formalism>");
		increaseIndent();
		indentedOutputln("<Formalism align:name=\""+((BasicAlignment)align).getOntologyObject2().getFormalism()+"\" align:uri=\""+((BasicAlignment)align).getOntologyObject2().getFormURI()+"\"/>");
		decreaseIndent();
		indentedOutputln("</formalism>");
	    }
	    decreaseIndent();
	    indentedOutputln("</Ontology>");
	    decreaseIndent();
	    indentedOutputln("</onto2>");
	}
	decreaseIndent();
	indentedOutputln("</Alignment>");
	decreaseIndent();
	indentedOutputln("</rdf:RDF>");
    }

    public void visit( Cell c ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, c, Cell.class ) ) return;
	// default behaviour
    };

    public void visit( Relation r ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, r, Relation.class ) ) return;
	// default behaviour
    };
    
}
