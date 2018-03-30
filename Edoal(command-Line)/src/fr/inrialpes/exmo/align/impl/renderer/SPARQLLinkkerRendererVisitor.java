/*
 * $Id: SPARQLLinkkerRendererVisitor.java 2157 2017-07-19 10:15:36Z euzenat $
 *
 * Copyright (C) INRIA, 2015-2017
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

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owl.align.Relation;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALAlignment;
import fr.inrialpes.exmo.align.impl.edoal.EDOALCell;
import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.ClassExpression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyExpression;
import fr.inrialpes.exmo.align.impl.edoal.Linkkey;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyBinding;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyEquals;
import fr.inrialpes.exmo.align.impl.edoal.LinkkeyIntersects;

import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Set;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 * The architecture of these SPARQL renderer does not comply with the classical renderer architecture
 * whose goal is to immediatelt display the query on given writer.
 *
 * It would be better to come back to it.
 * But beware, this will conflict with split (several output files)
 */

public class SPARQLLinkkerRendererVisitor extends GraphPatternRendererVisitor implements AlignmentVisitor {

    Alignment alignment = null;
    Cell cell = null;
    Hashtable<String, String> nslist = null;

    /* Named graph for the data sources (if none, null) */
    private String onto1NamedGraph, onto2NamedGraph = null;

    /* Name of variables to be used for the individual to match */
    private String lkvar1, lkvar2 = null;

    /* Name of the linking graph pattern */
    private String lkpattern = null;

    boolean embedded = false;

    boolean edoal = false;

    boolean select = false;

    boolean generateSameAs = false;

    boolean requestedblanks = false;

    private String content_Corese = "";                     // resulting string for Corese

    public SPARQLLinkkerRendererVisitor( PrintWriter writer ) {
        super(writer);
    }

    public void setGraph1Name( String name ) {
	onto1NamedGraph = name;
    }

    public void setGraph2Name( String name ) {
	onto2NamedGraph = name;
    }

    /**
     * Initialises the parameters of the renderer
     */
    public void init( Properties p ) {
	super.init( p );
        if ( p.getProperty("graphName1") != null ) {
            setGraph1Name( p.getProperty("graphName1") );
        }
        if ( p.getProperty("graphName2") != null ) {
            setGraph2Name( p.getProperty("graphName2") );
        }
        if ( p.getProperty("select") != null ) {
            select = true;
	}
        if ( p.getProperty("sameAs") != null ) {
            generateSameAs = true;
	}
	if ( generateSameAs || !select ) {
	    prefixList.put("http://www.w3.org/2002/07/owl#", "owl");
        }

        if (p.getProperty("embedded") != null
                && !p.getProperty("embedded").equals("")) {
            embedded = true;
        }
        if (p.getProperty("blanks") != null && !p.getProperty("blanks").equals("")) {
            requestedblanks = true;
        }
        if (p.getProperty("weakens") != null && !p.getProperty("weakens").equals("")) {
            weakens = true;
        }
        if (p.getProperty("ignoreerrors") != null && !p.getProperty("ignoreerrors").equals("")) {
            ignoreerrors = true;
        }
        if (p.getProperty("corese") != null && !p.getProperty("corese").equals("")) {
            corese = true;
        }

        split((p.getProperty("split") != null && !p.getProperty("split").equals("")), p.getProperty("dir") + "/");

        if (p.getProperty("indent") != null) {
            INDENT = p.getProperty("indent");
        }
        if (p.getProperty("newline") != null) {
            NL = p.getProperty("newline");
        }
    }

    public void visit( Alignment align ) throws AlignmentException {
        if (subsumedInvocableMethod(this, align, Alignment.class)) {
            return;
        }
        if (align instanceof EDOALAlignment) {
            alignment = align;
        } else {
            try {
                alignment = EDOALAlignment.toEDOALAlignment((BasicAlignment) align);
            } catch (AlignmentException alex) {
                throw new AlignmentException("SPARQLLinkerRenderer: cannot render simple alignment. Need an EDOALAlignment", alex);
            }
        }
        edoal = alignment.getLevel().startsWith("2EDOAL");
	if ( !edoal ) throw new AlignmentException("SPARQLLinkerRenderer: cannot render simple alignment. Need an EDOALAlignment" );
        content_Corese = "<?xml version=\"1.0\" encoding=\""+ENC+"\"?>" + NL;
        content_Corese += "<!DOCTYPE rdf:RDF [" + NL;
        content_Corese += "<!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" + NL;
        content_Corese += "<!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\">" + NL;
        content_Corese += "<!ENTITY rul \"http://ns.inria.fr/edelweiss/2011/rule#\">" + NL;
        content_Corese += "]>" + NL;
        content_Corese += "<rdf:RDF xmlns:rdfs=\"&rdfs;\" xmlns:rdf=\"&rdf;\" xmlns = \'&rul;\' >" + NL + NL + NL;
        for ( Cell c : alignment ) {
            c.accept( this );
        };
        content_Corese += "</rdf:RDF>" + NL;
        if ( corese ) {
            saveQuery( align, content_Corese );
        }
    }

    public void visit( Cell cell ) throws AlignmentException {
        if ( subsumedInvocableMethod(this, cell, Cell.class) ) {
            return;
        }
        // default behaviour
        this.cell = cell;
	Expression e1 = (Expression)(cell.getObject1());
	Expression e2 = (Expression)(cell.getObject2());
	if ( ! (e1 instanceof ClassExpression) || ! (e2 instanceof ClassExpression) ) return;
	ClassExpression expr1 = (ClassExpression)e1;
	ClassExpression expr2 = (ClassExpression)e2;
	// This should necessarily be cells with linkkeys
        Set<Linkkey> linkkeys = ((EDOALCell)cell).linkkeys();
        if ( linkkeys == null || linkkeys.isEmpty() ) return;

        String query = "";
	for ( Linkkey linkkey : linkkeys ) {
	    // Here the generation is dependent on global variables
	    blanks = true;
	    resetVariables( expr1, "s1", "o1" );
	    expr1.accept( this );
	    String GP1 = wrapInNamedGraph( onto1NamedGraph, getGP() );
	    List<String> listGP1 = new ArrayList<String>(getBGP());
	    blanks = requestedblanks;
	    resetVariables( expr2, "s2", "o2" );
	    expr2.accept( this );
	    String GP2 = wrapInNamedGraph( onto2NamedGraph, getGP() );

	    // Generate linkkey part!
	    // JE2015: only works if only one linkkey
	    lkvar1 = "?s1";
	    lkvar2 = "?s2";
	    linkkey.accept( this );
	    // End of global variables
	    if ( !GP1.contains("UNION") && !GP1.contains("FILTER") ) {
		query += createQuery( "?s1", GP1, "?s2", GP2, lkpattern ) + NL;
		if (corese) content_Corese += createCoreseQuery( query );
	    } else if (weakens) {
		String tmp = "";
		for (String str : listGP1) {
		    if (!str.contains("UNION") && !str.contains("FILTER")) {
			tmp += str;
		    }
		}
		if (!tmp.equals("")) {
		    query += createQuery( "?s1", tmp, "?s2", GP2, lkpattern ) + NL;
		}
	    } else if (ignoreerrors) {
		query += createQuery( "?s1", GP1, "?s2", GP2, lkpattern ) + NL;
	    }
	    if (corese) return;
	}
        saveQuery(cell, query);
    }

    public void visit(Relation rel) throws AlignmentException {
        if (subsumedInvocableMethod(this, rel, Relation.class)) {
            return;
        }
        // default behaviour
        // rel.write( writer );
    }

    protected String createQuery( String v1, String GP1, String v2, String GP2, String LKPat ) {
	if ( select ) return createSelect( v1, GP1, v2, GP2, LKPat );
	else return createLinkConstruct( v1, GP1, v2, GP2, LKPat );
    }

    protected String createLinkConstruct( String v1, String GP1, String v2, String GP2, String LKPat ) {
	return createPrefixList() + NL + "CONSTRUCT { " + v1 + " owl:sameAs " + v2 + " }" + NL + "WHERE {" + NL + GP1 + GP2 + LKPat + "}" + NL;
    }

    /* Different from the one in Select */
    protected String createSelect( String v1, String GP1, String v2, String GP2, String LKPat ) {
	return createPrefixList() + NL + "SELECT DISTINCT " + v1 + " " + v2 + NL + "WHERE {" + NL + GP1 + GP2 + LKPat + "}" + NL;
    }

    protected String createCoreseQuery( String query ) {
        return "<rule>" + NL + "<body>" + NL + "<![CDATA[" + NL + query + "]]>" + NL + "</body>" + NL + "</rule>" + NL + NL;
    }

    public void visit( final Linkkey linkkey ) throws AlignmentException {
	lkpattern = "";
        for( LinkkeyBinding linkkeyBinding : linkkey.bindings() ) {
            linkkeyBinding.accept(this);
        }
    }

    /**
     * Generate the constraint corresponding to:
     * MINUS { ?x pj ?wj . FILTER NOT EXISTS { ?y qj ?wj . } }
     * MINUS { ?y qj ?wj . FILTER NOT EXISTS { ?x pj ?wj . } }
     * Unfortunately with real link keys it should be (for w1 w2 initially selected):
     * MINUS { ?x pj ?w3 . FILTER ( NOT ( EXISTS { ?y qj ?w4 . } &amp;&amp;  lcase(str(?w3)) = lcase(str(?w4)) )) }
     * MINUS { ?y qj ?w5 . FILTER ( NOT ( EXISTS { ?x pj ?w6 . } &amp;&amp;  lcase(str(?w6)) = lcase(str(?w5)) )) }
     */
    public void visit( final LinkkeyEquals linkkeyEquals ) throws AlignmentException {
	// In the semantics of EQ-Linkkeys, the intersection must not be empty...
	processInLinkKey( linkkeyEquals );
	if ( (linkkeyEquals.getExpression1() instanceof PropertyExpression) || (linkkeyEquals.getExpression2() instanceof PropertyExpression) ) { // Literal values
	    //initStructure(); //strBGP = ""; // congrats! Unsure
	    emptyGP();
	    String o1 = createVarName();
	    resetVariables( "?s1", o1 );
	    linkkeyEquals.getExpression1().accept( this );
	    String GP1 = wrapInNamedGraph( onto1NamedGraph, getGP() );
	    //initStructure(); //strBGP = ""; // congrats!
	    emptyGP();
	    String o2 = createVarName();
	    resetVariables( "?s2", o2 );
	    linkkeyEquals.getExpression2().accept( this );
	    String GP2 = wrapInNamedGraph( onto2NamedGraph, getGP() );
	    // The two patterns are independent so can use the same variables
	    lkpattern += "MINUS { " + GP1 + "FILTER NOT EXISTS { "+ GP2 +" FILTER ( "+equalValueFilterStatement( o1, o2 )+" ) } }"+NL;
	    lkpattern += "MINUS { " + GP2 + "FILTER NOT EXISTS { "+ GP1 +" FILTER ( "+equalValueFilterStatement( o1, o2 )+" ) } }"+NL;
	} else if ( generateSameAs ) { // should be the same...
	    //initStructure(); //strBGP = ""; // congrats! Unsure
	    emptyGP();
	    String o1 = createVarName();
	    resetVariables( "?s1", o1 );
	    linkkeyEquals.getExpression1().accept( this );
	    String GP1 = wrapInNamedGraph( onto1NamedGraph, getGP() );
	    //initStructure(); //strBGP = ""; // congrats!
	    emptyGP();
	    String o2 = createVarName();
	    resetVariables( "?s2", o2 );
	    linkkeyEquals.getExpression2().accept( this );
	    String GP2 = wrapInNamedGraph( onto2NamedGraph, getGP() );
	    // The two patterns are independent so can use the same variables
	    lkpattern += "MINUS { " + GP1 + "FILTER NOT EXISTS { "+ GP2 +" FILTER ( "+equalObjectFilterStatement( o1, o2 )+" ) } }"+NL;
	    lkpattern += "MINUS { " + GP2 + "FILTER NOT EXISTS { "+ GP1 +" FILTER ( "+equalObjectFilterStatement( o1, o2 )+" ) } }"+NL;
	} else {
	    initStructure(); //strBGP = ""; // congrats! Unsure
	    emptyGP();
	    String o1 = createVarName();
	    resetVariables( "?s1", o1 );
	    linkkeyEquals.getExpression1().accept( this );
	    String temp = obj;
	    String GP1 = wrapInNamedGraph( onto1NamedGraph, getGP() );
	    initStructure(); //strBGP = ""; // congrats!
	    resetVariables( "?s2", temp );
	    linkkeyEquals.getExpression2().accept( this );
	    String GP2 = wrapInNamedGraph( onto2NamedGraph, getGP() );
	    // The two patterns are independent so can use the same variables
	    lkpattern += "MINUS { " + GP1 + " FILTER NOT EXISTS { " + GP2 + " } }"+NL;
	    lkpattern += "MINUS { " + GP2 + " FILTER NOT EXISTS { " + GP1 + " } }"+NL;
	}
   }

    public void visit( final LinkkeyIntersects linkkeyIntersects ) throws AlignmentException {
	processInLinkKey( linkkeyIntersects );
    }

    public void processInLinkKey( final LinkkeyBinding binding ) throws AlignmentException {
	if ( (binding.getExpression1() instanceof PropertyExpression) || (binding.getExpression2() instanceof PropertyExpression) ) {
	    //Literal values
	    emptyGP();
	    String o1 = createVarName();
	    resetVariables( lkvar1, o1 );
	    binding.getExpression1().accept( this );
	    lkpattern += wrapInNamedGraph( onto1NamedGraph, getGP() );
	    emptyGP();
	    String o2 = createVarName();
	    resetVariables( lkvar2, o2 );
	    binding.getExpression2().accept( this );
	    lkpattern += wrapInNamedGraph( onto2NamedGraph, getGP() );
	    lkpattern += "FILTER ( "+equalValueFilterStatement( o1, o2 )+" )"+NL;
	    //"FILTER( lcase(str("+o1+")) = lcase(str("+o2+")) )"+NL;
	} else if ( generateSameAs ) {
	    // Relations
	    emptyGP();
	    String o1 = createVarName();
	    resetVariables( lkvar1, o1 );
	    binding.getExpression1().accept( this );
	    lkpattern += wrapInNamedGraph( onto1NamedGraph, getGP() );
	    emptyGP();
	    String o2 = createVarName();
	    resetVariables( lkvar2, o2 );
	    binding.getExpression2().accept( this );
	    lkpattern += wrapInNamedGraph( onto2NamedGraph, getGP() );
	    lkpattern += "FILTER ( "+equalObjectFilterStatement( o1, o2 )+" )"+NL;
	    //o1+" owl:sameAs "+o2+"."+NL+" UNION FILTER( "+o1+" = "+o2+" )"+NL;
	} else {
	    //?x p'i ?zi . ?y q'i ?zi .
	    emptyGP();
	    String o1 = createVarName();
	    resetVariables( lkvar1, o1 );
	    binding.getExpression1().accept( this );
	    lkpattern += wrapInNamedGraph( onto1NamedGraph, getGP() );
	    emptyGP();
	    resetVariables( lkvar2, o1 );
	    binding.getExpression2().accept( this );
	    lkpattern += wrapInNamedGraph( onto2NamedGraph, getGP() );
	}
    }

    // There could be two options, test which one is fastest to evaluate...
    // Note that the sameAs statement is evaluated out of any namedGraph
    // it could also be disjunctively wrapped in both named graphs
    protected String equalObjectFilterStatement ( String o1, String o2 ) {
	//return o1+" owl:sameAs "+o2+". UNION FILTER( "+o1+" = "+o2+" )";
	return o1+" = "+o2+" || EXISTS { "+o1+" owl:sameAs "+o2+" . }";
    }
    
    protected String equalValueFilterStatement ( String o1, String o2 ) {
	return "lcase(str("+o1+")) = lcase(str("+o2+"))";
    }
    
    protected String wrapInNamedGraph( String namedGraph, String stuff ) {
        if ( namedGraph != null ) {
            return "GRAPH <" + namedGraph + "> {" + NL + stuff + "}" + NL ;
	} else {
	    return stuff;
	}
    }

}
