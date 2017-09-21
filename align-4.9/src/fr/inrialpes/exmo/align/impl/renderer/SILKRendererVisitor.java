/*
 * $Id: SILKRendererVisitor.java 2102 2015-11-29 10:29:56Z euzenat $
 *
 * Copyright (C) INRIA, 2012, 2014-2015
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

import java.io.PrintWriter;
import java.net.URI;
import java.util.Set;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.ontowrap.Ontology;

import fr.inrialpes.exmo.align.impl.Namespace;
import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.PathExpression;
import fr.inrialpes.exmo.align.impl.edoal.RelationConstruction;
import fr.inrialpes.exmo.align.impl.edoal.RelationExpression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyConstruction;
import fr.inrialpes.exmo.align.impl.edoal.RelationId;
import fr.inrialpes.exmo.align.impl.edoal.PropertyId;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyBinding;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyEquals;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyIntersects;
import fr.inrialpes.exmo.align.parser.SyntaxElement.Constructor;

/**
 * This renders only a skeleton for the SILK script.
 * In particular, it is impossible to know where to fetch the data from (this could eventually be injected)
 */
public class SILKRendererVisitor extends GraphPatternRendererVisitor implements AlignmentVisitor {
    final static Logger logger = LoggerFactory.getLogger(SILKRendererVisitor.class);

    Alignment alignment = null;
    Cell cell = null;
    Hashtable<String,String> nslist = null;

    private boolean embedded = false;

    private String directory = "";
    private String threshold = "";
    private String limit = "";
    private Random rand;
    
    public SILKRendererVisitor(PrintWriter writer) {
	super(writer);
    }   

    public void init( Properties p ) {
	if ( p.getProperty( "embedded" ) != null && !p.getProperty( "embedded" ).equals("") ) 
	    embedded = true;
	if ( p.getProperty( "directory" ) != null && !p.getProperty( "directory" ).equals("") ) 
	    directory = p.getProperty( "directory" );
	if ( p.getProperty( "blanks" ) != null && !p.getProperty( "blanks" ).equals("") ) 
	    blanks = true;
	if ( p.getProperty( "weakens" ) != null && !p.getProperty( "weakens" ).equals("") ) 
	    weakens = true;
	if ( p.getProperty( "ignoreerrors" ) != null && !p.getProperty( "ignoreerrors" ).equals("") ) 
	    ignoreerrors = true;
	if ( p.getProperty( "silkthreshold" ) != null && !p.getProperty( "silkthreshold" ).equals("") ) {
	    threshold = " threshold=\""+p.getProperty( "silkthreshold" )+"\"";
	}
	if ( p.getProperty( "silklimit" ) != null && !p.getProperty( "silklimit" ).equals("") ) {
	    limit = " limit=\""+p.getProperty( "silklimit" )+"\"";
	}
	if ( p.getProperty( "indent" ) != null )
	    INDENT = p.getProperty( "indent" );
	if ( p.getProperty( "newline" ) != null )
	    NL = p.getProperty( "newline" );
	rand = new Random( System.currentTimeMillis() );
    }

    public void visit(Alignment align) throws AlignmentException {
    	if ( subsumedInvocableMethod( this, align, Alignment.class ) ) return;
    	// default behaviour
    	String extensionString = "";
    	alignment = align;
    	nslist = new Hashtable<String,String>();
	nslist.put( Namespace.RDF.prefix , Namespace.RDF.shortCut );
	nslist.put( Namespace.XSD.prefix , Namespace.XSD.shortCut );
    	// Get the keys of the parameter
    	int gen = 0;
    	for ( String[] ext : align.getExtensions() ) {
    	    String prefix = ext[0];
    	    String name = ext[1];
    	    String tag = nslist.get(prefix);
    	    //if ( tag.equals("align") ) { tag = name; }
    	    if ( prefix.equals( Namespace.ALIGNMENT.uri ) ) { tag = name; }
    	    else {
    		if ( tag == null ) {
    		    tag = "ns"+gen++;
    		    nslist.put( prefix, tag );
    		}
    		tag += ":"+name;
    	    }
    	    extensionString += INDENT+"<"+tag+">"+ext[2]+"</"+tag+">"+NL;
    	}
    	if ( embedded == false ) {
    	    indentedOutputln("<?xml version='1.0' encoding='utf-8' standalone='no'?>"+NL+NL);
    	}
    	indentedOutputln("<Silk>");
	increaseIndent();
    	indentedOutputln("<Prefixes>");
	increaseIndent();
    	indentedOutputln("<Prefix id=\"rdf\" namespace=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" />");
    	indentedOutputln("<Prefix id=\"rdfs\" namespace=\"http://www.w3.org/2000/01/rdf-schema#\" />");
    	indentedOutputln("<Prefix id=\"owl\" namespace=\"http://www.w3.org/2002/07/owl#\" />");
	// JE2014: BUG
	// These prefix are usually added by the GraphPatternGenerator after the patterns are generated...
	// So, now they are empty...
	for ( Entry<String,String> e : prefixList.entrySet() ) {
	    indentedOutputln("<Prefix id=\""+e.getKey()+" namespace=\""+e.getValue()+"\" />");
	}
	decreaseIndent();
    	indentedOutputln("</Prefixes>"+NL);
    	indentedOutputln("<DataSources>");
	increaseIndent();
	indentedOutputln("<!-- These may have to be edited to proper data sources -->");
	if ( align instanceof BasicAlignment ) {
	    printOntology( ((BasicAlignment)align).getOntologyObject1(), "source" );
	} else {
	    printBasicOntology( align.getOntology1URI(), align.getFile1(), "source" );
	}
	if ( align instanceof BasicAlignment ) {
	    printOntology( ((BasicAlignment)align).getOntologyObject2(), "target" );
	} else {
	    printBasicOntology( align.getOntology2URI(), align.getFile2(), "target" );
	}
	decreaseIndent();
    	indentedOutputln("</DataSources>"+NL);
    	indentedOutputln("<Interlinks>");
	increaseIndent();
    	for( Cell c : align ){ c.accept( this ); };
	// JE2014: ONLY NOW PREFIX SHOULD BE OK!
    	decreaseIndent();
    	indentedOutputln("</Interlinks>");
    	decreaseIndent();
    	outputln("</Silk>");
    }

    private void printBasicOntology ( URI u, URI f, String function ) {
	indentedOutput("<DataSource id=\""+function+"\" type=\"file\">"+NL);
	increaseIndent();
	if ( f != null ) {
	    indentedOutputln("<Param name=\"file\" value=\""+f+"\" />");
	} else {
	    indentedOutputln("<Param name=\"file\" value=\""+u+"\" />");
	}
	indentedOutputln("<Param name=\"format\" value=\"RDF/XML\" />");
	decreaseIndent();
    	indentedOutputln("</DataSource>");
    }

    public void printOntology( Ontology<Object> onto, String function ) {
	URI u = onto.getURI();
	URI f = onto.getFile();
	printBasicOntology( u, f, function );
    }

    public void visit( Cell cell ) throws AlignmentException {
    	if ( subsumedInvocableMethod( this, cell, Cell.class ) ) return;
    	// default behaviour
    	this.cell = cell;      	

	// JE: cannot use Cell id because it is an URI and not an id
	String id = "RandomId"+Math.abs( rand.nextInt( 100000 ) );
    	
    	URI u1 = cell.getObject1AsURI(alignment);
    	URI u2 = cell.getObject2AsURI(alignment);
    	if ( ( u1 != null && u2 != null)
    	     || (alignment instanceof EDOALAlignment) ){ 
		
	    indentedOutputln("<Interlink id=\""+id+"\">");
	    increaseIndent();
	    indentedOutputln("<LinkType>owl:sameAs</LinkType>");
	    indentedOutputln("<SourceDataset dataSource=\"source\"" + " var=\"s\">");
	    increaseIndent();
	    indentedOutputln("<RestrictTo>");
	    increaseIndent();
	    resetVariables( (Expression)(cell.getObject1()), "s", "o" );
	    ((Expression)(cell.getObject1())).accept( this );
	    indentedOutput(getGP());
	    decreaseIndent();
	    indentedOutputln("</RestrictTo>");
	    decreaseIndent();
	    indentedOutputln("</SourceDataset>");
	    
	    indentedOutputln("<TargetDataset dataSource=\"target\"" + " var=\"x\">");
	    increaseIndent();
	    indentedOutputln("<RestrictTo>");
	    increaseIndent();
	    resetVariables( (Expression)(cell.getObject2()), "x", "y" );	    		
	    ((Expression)(cell.getObject2())).accept( this );
	    indentedOutput(getGP());
	    decreaseIndent();
	    indentedOutputln("</RestrictTo>");
	    decreaseIndent();
	    indentedOutputln("</TargetDataset>");
	    
	    indentedOutputln("<LinkageRule>");
	    increaseIndent();
	    boolean treatedWithLinkkey = false;
	    if ( alignment instanceof EDOALAlignment ) {
		Set<Linkkey> linkkeys = ((EDOALCell)cell).linkkeys();
		if ( linkkeys != null && !linkkeys.isEmpty() ) { // Use link keys if available
		    treatedWithLinkkey = true;
		    // If size == 1, just generate the linkkey
		    if ( linkkeys.size() == 1 ) {
			linkkeys.iterator().next().accept( this );
		    } else { // else aggregate with max
			indentedOutputln("<Aggregate type=\"max\">");
			increaseIndent();
			for ( Linkkey linkkey : linkkeys ) {
			    linkkey.accept( this );
			}
			decreaseIndent();
			indentedOutputln("</Aggregate>");
		    }
		}
	    }
	    if ( !treatedWithLinkkey ) { // Default action: in principle, it would be possible to use properties
		indentedOutputln("<Compare metric=\"levenshtein\" threshold=\".5\">");
		increaseIndent();
		indentedOutputln("<TransformInput function=\"stripUriPrefix\">");
		increaseIndent();
		indentedOutputln("<Input path=\"?s\" />");
		decreaseIndent();
		indentedOutputln("</TransformInput>");
		indentedOutputln("<TransformInput function=\"stripUriPrefix\">");
		increaseIndent();
		indentedOutputln("<Input path=\"?x\" />");
		decreaseIndent();
		indentedOutputln("</TransformInput>");
		decreaseIndent();
		indentedOutputln("</Compare>");
	    }
	    decreaseIndent();
	    indentedOutputln("</LinkageRule>");
	    indentedOutputln("<Filter"+threshold+limit+" />");
	    indentedOutputln("<Outputs>");	    		
	    increaseIndent();
	    indentedOutputln("<Output minConfidence=\".7\" type=\"file\">");
	    increaseIndent();
	    indentedOutputln("<Param name=\"file\" value=\""+id+"-accepted.nt\"/>");
	    indentedOutputln("<Param name=\"format\" value=\"ntriples\"/>");
	    decreaseIndent();
	    indentedOutputln("</Output>");
	    indentedOutputln("<Output maxConfidence=\".7\" minConfidence=\".2\" type=\"file\">");
	    increaseIndent();
	    indentedOutputln("<Param name=\"file\" value=\""+id+"-tocheck.nt\"/>");
	    indentedOutputln("<Param name=\"format\" value=\"ntriples\"/>");
	    decreaseIndent();
	    indentedOutputln("</Output>");
	    decreaseIndent();
	    indentedOutputln("</Outputs>");
	    decreaseIndent();
	    indentedOutputln("</Interlink>"+NL);		    		
    	}
    }

    public void visit( Relation rel ) throws AlignmentException {
	if ( subsumedInvocableMethod( this, rel, Relation.class ) ) return;
	// Default behaviour
	// rel.write( writer );
    }
    public void visit(final Linkkey linkkey) throws AlignmentException {
	indentedOutputln("<Aggregate type=\"average\">"); // tentative
	increaseIndent();
        for( LinkkeyBinding linkkeyBinding : linkkey.bindings() ) {
            linkkeyBinding.accept(this);
        }
	decreaseIndent();
	indentedOutputln("</Aggregate>");
    }
    
    public void visit(final LinkkeyBinding binding) throws AlignmentException {
	indentedOutputln("<Compare metric=\"levenshtein\" threshold=\".5\">");
	//increaseIndent();
	//indentedOutputln("<TransformInput function=\"stripUriPrefix\">");
	increaseIndent();
	String pathexpr = generateSILKPath( "?s", binding.getExpression1() );
	indentedOutputln("<Input path=\""+pathexpr+"\" />");
	//decreaseIndent();
	//indentedOutputln("</TransformInput>");
	//indentedOutputln("<TransformInput function=\"stripUriPrefix\">");
	//increaseIndent();
	pathexpr = generateSILKPath( "?x", binding.getExpression2() );
	indentedOutputln("<Input path=\""+pathexpr+"\" />");
	//decreaseIndent();
	//indentedOutputln("</TransformInput>");
	decreaseIndent();
	indentedOutputln("</Compare>");
    }

    /**
     * This is incomplete as SILK is able to deal with reverse (\) and constraints ([])
     * 
     * @param var: the starting point of the path (a variable)
     * @param pex: the path expression to render
     * @return the rendering of the path expression
     * @throws AlignmentException when something goes wrong
     */
    private String generateSILKPath( String var, PathExpression pex ) throws AlignmentException {
	String path = var;
	if ( pex instanceof RelationId ) {
	    path += "/"+registerPrefix( ((RelationId)pex).getURI() );
	} else if ( pex instanceof PropertyId ) {
	    path += "/"+registerPrefix( ((PropertyId)pex).getURI() );
	} else if ( pex instanceof RelationConstruction && ((RelationConstruction)pex).getOperator() == Constructor.COMP ) {
	    for ( RelationExpression relexp : ((RelationConstruction)pex).getComponents() ) {
		if ( relexp instanceof RelationId ) {
		    path += "/"+registerPrefix( ((RelationId)relexp).getURI() );
		} // else, we cannot do...
	    }
	} else if ( pex instanceof PropertyConstruction && ((PropertyConstruction)pex).getOperator() == Constructor.COMP ) {
	    for ( PathExpression pathexp : ((PropertyConstruction)pex).getComponents() ) {
		if ( pathexp instanceof RelationId ) {
		    path += "/"+registerPrefix( ((RelationId)pathexp).getURI() );
		} else if ( pathexp instanceof PropertyId ) {
		    path += "/"+registerPrefix( ((PropertyId)pathexp).getURI() );
		} // else, we cannot do...
	    }
	} // else we cannot do...
	return var;
    }
	
    public void visit(final LinkkeyEquals linkkeyEquals) throws AlignmentException {
        throw new AlignmentException("NOT IMPLEMENTED !");
    }
    public void visit(final LinkkeyIntersects linkkeyIntersects) throws AlignmentException {
        throw new AlignmentException("NOT IMPLEMENTED !");
    }
}
