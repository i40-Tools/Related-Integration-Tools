/*
 * $Id: GraphPatternRendererVisitor.java 2150 2017-07-18 15:15:46Z euzenat $
 *
 * Copyright (C) INRIA, 2012-2017
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.impl.edoal.Expression;
import fr.inrialpes.exmo.align.impl.edoal.Apply;
import fr.inrialpes.exmo.align.impl.edoal.Aggregate;
import fr.inrialpes.exmo.align.impl.edoal.ClassConstruction;
import fr.inrialpes.exmo.align.impl.edoal.ClassDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassExpression;
import fr.inrialpes.exmo.align.impl.edoal.ClassId;
import fr.inrialpes.exmo.align.impl.edoal.ClassOccurenceRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.ClassValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.Comparator;
import fr.inrialpes.exmo.align.impl.edoal.Datatype;
import fr.inrialpes.exmo.align.impl.edoal.EDOALVisitor;
import fr.inrialpes.exmo.align.impl.edoal.InstanceExpression;
import fr.inrialpes.exmo.align.impl.edoal.InstanceId;
import fr.inrialpes.exmo.align.impl.edoal.PathExpression;
import fr.inrialpes.exmo.align.impl.edoal.PropertyConstruction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyId;
import fr.inrialpes.exmo.align.impl.edoal.PropertyTypeRestriction;
import fr.inrialpes.exmo.align.impl.edoal.PropertyValueRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationCoDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationConstruction;
import fr.inrialpes.exmo.align.impl.edoal.RelationDomainRestriction;
import fr.inrialpes.exmo.align.impl.edoal.RelationId;
import fr.inrialpes.exmo.align.impl.edoal.Transformation;
import fr.inrialpes.exmo.align.impl.edoal.Value;
import fr.inrialpes.exmo.align.impl.edoal.ValueExpression;
import fr.inrialpes.exmo.align.parser.SyntaxElement.Constructor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Translate correspondences into Graph Patterns
 *
 */
// JE: create a string... problem with increment.
public abstract class GraphPatternRendererVisitor extends IndentedRendererVisitor implements EDOALVisitor {
    final static Logger logger = LoggerFactory.getLogger(GraphPatternRendererVisitor.class);

    Alignment alignment = null;
    Cell cell = null;
    Hashtable<String, String> nslist = null;

    protected boolean ignoreerrors = false;
    protected boolean blanks = false;
    protected boolean weakens = false;
    protected boolean corese = false;

    private boolean inClassRestriction = false;
    private Object valueRestriction = null;
    private boolean flagRestriction;

    private String instance = null;
    private String value = "";
    private String uriType = null;
    private String datatype = "";
    private Constructor op = null;
    private Integer nbCardinality = null;
    private String opOccurence = "";
    private int numberNs = 0;
    private int fileIndex = 1;
    private String sub = "";
    private boolean rel = false;
    protected String obj = "";
    private String strBGP = "";
    protected List<String> listBGP = null;
    private Set<String> subjectsRestriction = null;

    private Set<String> objectsRestriction = null;
    protected Hashtable<String, String> prefixList = null;

    protected int varsIndexcount = 1;

    private boolean split = false;                                  // split each query in a file, not on the writer
    private String splitdir = "";                                   // directory where to put query files
    protected HashMap<Object, String> queries;                      // Store with queries corresponding to cells

    public GraphPatternRendererVisitor( PrintWriter writer ) {
        super( writer );
    }

    protected void init( Properties p ) {
	super.init( p );
        listBGP = new ArrayList<String>();
        subjectsRestriction = new HashSet<String>();
        objectsRestriction = new HashSet<String>();
	numberNs = 0;
        prefixList = new Hashtable<String, String>();
        prefixList.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
        prefixList.put("http://www.w3.org/2001/XMLSchema#", "xsd"); // used in types
        queries = new LinkedHashMap<Object, String>();
	initStructure();
    }

    protected void initStructure() {
        varsIndexcount = 1;
        strBGP = "";
        listBGP.clear();
        objectsRestriction.clear();
        flagRestriction = false;
    }
    
    protected void resetVariables(Expression expr, String s, String o) throws AlignmentException {
        initStructure();
        if ( expr instanceof ClassExpression ) {
	    resetVariables("?" + s, createVarName());
        } else if ( expr instanceof PathExpression ) {
	    resetVariables("?" + s, "?" + o);
        } else if ( expr instanceof InstanceExpression ) {
	    resetVariables("?" + s, createVarName());
        } else {
            throw new AlignmentException("Cannot render as query : " + expr);
        }
    }

    protected void resetVariables( String s, String o ) {
        sub = s;
        obj = o;
    }

    /**
     * Generates a new variable. This is always an object because we know 
     * the subjects (named by outside).
     * It may be necessary to generate blanks.
     *
     * @return the new variable
     */
    protected String createVarName() {
        if ( blanks ) {
            obj = "_:o" + ++varsIndexcount;
        } else {
            obj = "?o" + ++varsIndexcount;
        }
        return obj;
    }

    protected String createPropertyVarName() {
        if ( blanks ) {
            return "_:o" + ++varsIndexcount;
        } else {
            return "?o" + ++varsIndexcount;
        }
    }

    protected void addToGP(String str) {
        strBGP += str;
    }

    protected String getGP() {
        return strBGP;
    }

    protected void emptyGP() {
        strBGP = "";
    }

    protected List<String> getBGP() {
        return listBGP;
    }

    public String registerPrefix( URI u ) {
        String str = u.toString();
        int index;
        if ( str.contains("#") ) index = str.lastIndexOf("#");
        else index = str.lastIndexOf("/");
	return getOrGenerateNSPrefix(str.substring(0, index + 1)) + ":" + str.substring(index + 1);
    }

    public String getPrefixDomain( URI u ) {
        String str = u.toString();
        int index;
        if (str.contains("#")) {
            index = str.lastIndexOf("#");
        } else {
            index = str.lastIndexOf("/");
        }
        return str.substring(0, index + 1);
    }

    public String getPrefixName(URI u) { // Would better be suffix
        String str = u.toString();
        int index;
        if (str.contains("#")) {
            index = str.lastIndexOf("#");
        } else {
            index = str.lastIndexOf("/");
        }
        return str.substring(index + 1);
    }

    public String getOrGenerateNSPrefix( String namespace ) {
        if ( namespace.length() == 0 ) return "";
        String ns = prefixList.get( namespace );
        if (ns == null) {
            prefixList.put( namespace, ns = "ns" + numberNs++ );
        }
        return ns;
    }

    protected final void split(boolean split, String splitdir) {
        if (split) {
            this.split = split;
            this.splitdir = splitdir;
        } else {
            this.split = false;
            this.splitdir = null;
        }
    }

    public void saveQuery(Object referer, String query) {
        //Query is stored in memory
	queries.put( referer, query );
        if ( split ) {
            BufferedWriter out = null;
            try {
                FileWriter writer = new FileWriter(splitdir + "query" + fileIndex + ".rq");
                out = new BufferedWriter(writer);
                fileIndex++;
                out.write(query);
            } catch (IOException ioe) {
                logger.debug("IGNORED Exception", ioe);
            } finally {
		try {
		    if ( out != null ) out.close(); // there was at least one file
		} catch (IOException ioe) {};
	    }
        } else {
            writer.println(query);
        }
    }

    /**
     * Produce Query only for local call.
     *
     * @param referer: the reference to a query (corresponding to a correspondence)
     * @return the query
     */
    public String getQuery( Object referer ) {
        return queries.get( referer );
    }

    // Obsoleted -> use inverse() instead
    /*
    public String getQueryFromOnto1ToOnto2(Object referer) {
        return getQuery(referer, 1);
    }

    public String getQueryFromOnto2ToOnto1(Object referer) {
        return getQuery(referer, 0);
    }*/

    protected String createPrefixList() {
        String result = "";
	for ( Entry<String,String> e : prefixList.entrySet() ) {
            result += "PREFIX " + e.getValue() + ":<" + e.getKey() + ">" + NL;
        }
        return result;
    }

    public void visit(final ClassId e) throws AlignmentException {
        if (e.getURI() != null) {
            String id = registerPrefix(e.getURI());
            if (!subjectsRestriction.isEmpty()) {
                Iterator<String> listSub = subjectsRestriction.iterator();
                while (listSub.hasNext()) {
                    String str = listSub.next();
                    strBGP += str + " rdf:type " + id + " ." + NL;
                }
                subjectsRestriction.clear();
            } else {
                strBGP += sub + " rdf:type " + id + " ." + NL;
            }
        }
    }

    public void visit( final ClassConstruction e ) throws AlignmentException {
        op = e.getOperator();
        if (op == Constructor.OR) {
            int size = e.getComponents().size();
            for (final ClassExpression ce : e.getComponents()) {
                strBGP += "{" + NL;
                ce.accept(this);
                size--;
                if (size != 0) {
                    strBGP += "}" + " UNION " + NL;
                } else {
                    strBGP += "}" + NL;
                }
            }
        } else if (op == Constructor.NOT) {
            strBGP += "FILTER (NOT EXISTS {";
            for (final ClassExpression ce : e.getComponents()) {
                ce.accept(this);
            }
            strBGP += "})" + NL;
        } else {
            for (final ClassExpression ce : e.getComponents()) {
                ce.accept(this);
                if (weakens && !strBGP.equals("")) {
                    listBGP.add(strBGP);
                    strBGP = "";
                }
            }
        }
    }

    private String valueToCompare( ValueExpression val, String var ) throws AlignmentException {
        value = "";
        uriType = "";
        flagRestriction = true;
        val.accept(this);
        flagRestriction = false;
	return "xsd:"+uriType+"( "+var+" )";
    }

    public void visit(final ClassValueRestriction c) throws AlignmentException {
	String str = "";
	instance = "";
	value = "";
	flagRestriction = true;
	c.getValue().accept(this);
	flagRestriction = false;
	//String xxxx = valueToCompare( c.getValue() );
	
	if (!instance.equals("")) {
	    valueRestriction = instance;
	} else if (!value.equals("")) {
	    valueRestriction = value;
	}
	
	opOccurence = c.getComparator().getSPARQLComparator();
	inClassRestriction = true;

	flagRestriction = true;
	c.getRestrictionPath().accept(this);
	flagRestriction = false;

	String temp = obj;
	if (inClassRestriction && !objectsRestriction.isEmpty()) {
	    Iterator<String> listObj = objectsRestriction.iterator();
	    if (op == Constructor.COMP) {
		String tmp = "";
		while (listObj.hasNext()) {
		    tmp = listObj.next();
		}
		str = "FILTER (" + tmp + opOccurence + valueRestriction + ")" + NL;
	    } else {
		while (listObj.hasNext()) {
		    str += "FILTER (" + listObj.next() + opOccurence + valueRestriction + ")" + NL;
		}
	    }
	    strBGP += str;
	}
	obj = temp;
	valueRestriction = null;
	inClassRestriction = false;
	if (op == Constructor.AND) {
	    createVarName();
	}
    }
    

    public void visit(final ClassTypeRestriction c) throws AlignmentException {
        String str = "";
        datatype = "";
        inClassRestriction = true;
        flagRestriction = true;
        c.getRestrictionPath().accept(this);
        flagRestriction = false;
        if (!objectsRestriction.isEmpty()) {
            Iterator<String> listObj = objectsRestriction.iterator();
            int size = objectsRestriction.size();
            if (size > 0) {
                str = "FILTER (datatype(" + listObj.next() + ") = ";
                visit(c.getType());
                str += "xsd:" + datatype;
            }
            while (listObj.hasNext()) {
                str += " && datatype(" + listObj.next() + ") = ";
                visit(c.getType());
                str += "xsd:" + datatype;
            }
            str += ")" + NL;

            strBGP += str;
        }
        objectsRestriction.clear();
        inClassRestriction = false;
    }

    public void visit(final ClassDomainRestriction c) throws AlignmentException {
        inClassRestriction = true;
        flagRestriction = true;
        c.getRestrictionPath().accept(this);
        flagRestriction = false;
        Iterator<String> listObj = objectsRestriction.iterator();
        while (listObj.hasNext()) {
            subjectsRestriction.add(listObj.next());
        }
        c.getDomain().accept(this);
        objectsRestriction.clear();
        inClassRestriction = false;
    }

    public void visit(final ClassOccurenceRestriction c) throws AlignmentException {
        String str = "";
        inClassRestriction = true;
	opOccurence = c.getComparator().getSPARQLComparator();
	nbCardinality = c.getOccurence();
        flagRestriction = true;
        c.getRestrictionPath().accept(this);
        flagRestriction = false;
        if (!objectsRestriction.isEmpty()) {
            Iterator<String> listObj = objectsRestriction.iterator();
            if ( op == Constructor.COMP ) {
                String tmp = "";
                while (listObj.hasNext()) {
                    tmp = listObj.next();
                }
                str += "FILTER( COUNT(" + tmp + ") " + opOccurence + " " + nbCardinality + ")" + NL;
            } else {
                while (listObj.hasNext()) {
                    str += "FILTER( COUNT(" + listObj.next() + ") " + opOccurence + " " + nbCardinality + ")" + NL;
                }
            }

            strBGP += str;
        }
        nbCardinality = null;
        opOccurence = "";
        inClassRestriction = false;
    }

    public void visit( final PropertyId e ) throws AlignmentException {
        if ( e.getURI() != null ) {
            String id = registerPrefix( e.getURI() );
            String temp = obj;
            if ( valueRestriction != null && !inClassRestriction && op != Constructor.COMP && flagRestriction ) {
		obj = valueRestriction.toString();
            }
            if ( flagRestriction && inClassRestriction ) {
                objectsRestriction.add(obj);
            }
            // createVarName(); //JE2014!
            strBGP += sub + " " + id + " " + obj + " ." + NL;
	    if ( e.getLanguage() != null ) {
		strBGP += "FILTER ( langMatches( lang( "+obj+" ), \""+e.getLanguage()+"\" ) )"+NL;
	    }
            obj = temp;
        }
    }

    public void visit( final PropertyConstruction e ) throws AlignmentException {
        op = e.getOperator();
	//System.err.println( "P: < "+sub+", "+op+", "+obj+" >" );
        if (op == Constructor.OR) {
            if ( valueRestriction != null && !inClassRestriction ) {
		obj = valueRestriction.toString();
            }
            int size = e.getComponents().size();
            for ( final PathExpression re : e.getComponents() ) {
                strBGP += "{" + NL;
                re.accept(this);
                size--;
                if (size != 0) {
                    strBGP += "}" + " UNION " + NL;
                } else {
                    strBGP += "}" + NL;
                }
            }
            objectsRestriction.add( obj );
        } else if (op == Constructor.AND) {
            if ( valueRestriction != null && !inClassRestriction ) {
		obj = valueRestriction.toString();
            }
            String temp = obj;
	    rel = false;
	    List<PathExpression> codomaintotreat = new ArrayList<PathExpression>();
            for ( final PathExpression re : e.getComponents() ) {
		if ( re instanceof PropertyTypeRestriction || re instanceof PropertyValueRestriction ) {
		    codomaintotreat.add( re );
		} else {
		    re.accept(this);
		    obj = temp;
		    rel = true;
		}
            }
	    for ( final PathExpression re : codomaintotreat ) {
		re.accept(this);
		obj = temp;
            }
            objectsRestriction.add( obj );
        } else if (op == Constructor.NOT) {
            strBGP += "FILTER (NOT EXISTS {" + NL;
            for (final PathExpression re : e.getComponents()) {
                re.accept(this);
            }
            strBGP += "})" + NL;
        } else if ( op == Constructor.COMP ) {
            int size = e.getComponents().size();
            String tempSub = sub;
            String tempObj = obj;
            for (final PathExpression re : e.getComponents()) {
                size--;
                // next object 
                if (size == 0) { // last step
                    if (valueRestriction != null && !inClassRestriction) {
			obj = valueRestriction.toString();
                    } else {
                        obj = tempObj;
                    } // otherwise, generating intermediate variables...
                } else if (blanks && (this.getClass() == SPARQLConstructRendererVisitor.class || this.getClass() == SPARQLLinkkerRendererVisitor.class )) {
                    obj = "_:o" + ++varsIndexcount;
                } else {
                    obj = "?o" + ++varsIndexcount;
                }
                // sub = last obj; obj = obj if last, var or blank otherwise
                re.accept(this); // p
                sub = obj; // sub <= last object
            }
            objectsRestriction.add(obj);
            obj = tempObj;
            sub = tempSub;
        } else {
	    // JE2015: Unclear here!
            if (valueRestriction != null && !inClassRestriction) {
		obj = valueRestriction.toString();
            }
            int size = e.getComponents().size();
            for (final PathExpression re : e.getComponents()) {
                re.accept(this);
                size--;
                objectsRestriction.add(obj);
                if (size != 0 && valueRestriction == null) {
                    //createVarName();
                    obj = "?o" + ++varsIndexcount;
                }
                if (weakens && !strBGP.equals("") && !inClassRestriction) {
                    listBGP.add(strBGP);
                    strBGP = "";
                }
            }
        }
        obj = "?o" + ++varsIndexcount;
    }

    public void visit( final PropertyValueRestriction c ) throws AlignmentException {
        String str = "";
	if ( !rel ) {
	    strBGP += sub+" "+createPropertyVarName()+" "+obj+" ." + NL;
	    rel = true; // only one is necessary
	}
	String val = valueToCompare( c.getValue(), obj );
	/*
        if (c.getComparator().getURI().equals(Comparator.EQUAL.getURI())) {
            str = "FILTER (" + val + " = ";
        } else if (c.getComparator().getURI().equals(Comparator.GREATER.getURI())) {
            str = "FILTER (" + val + " > ";
        } else {
            str = "FILTER (" + val + " < ";
        }
        str += "\"" + value + "\")" + NL;
	*/
	str = "FILTER (" + val + " " + c.getComparator().getSPARQLComparator() + " \"" + value + "\")" + NL;

        strBGP += str;
        value = "";
        uriType = "";
    }

    public void visit(final PropertyDomainRestriction c) throws AlignmentException {
        flagRestriction = true;
        c.getDomain().accept(this);
        flagRestriction = false;
    }

    public void visit(final PropertyTypeRestriction c) throws AlignmentException {
        String str = "";
	if ( !rel ) {
	    strBGP += sub+" "+createPropertyVarName()+" "+obj+" ." + NL;
	    rel = true; // only one is necessary
	}
        if ( !objectsRestriction.isEmpty() ) {
            Iterator<String> listObj = objectsRestriction.iterator();
            int size = objectsRestriction.size();
            if (size > 0) {
                str = "FILTER (datatype(" + listObj.next() + ") = ";
                visit( c.getType() );
                str += "xsd:" + datatype;
            }
            while ( listObj.hasNext() ) {
                str += " && datatype(" + listObj.next() + ") = ";
                visit( c.getType() );
                str += "xsd:" + datatype;
            }
            str += ")" + NL;
            strBGP += str;
        }
        objectsRestriction.clear();
    }

    public void visit(final RelationId e) throws AlignmentException {
        if (e.getURI() != null) {
            String id = registerPrefix(e.getURI());
            strBGP += sub + " " + id + "";
            if (op == Constructor.TRANSITIVE && flagRestriction) {
                strBGP += "*";
            }
            if (valueRestriction != null && !inClassRestriction && op != Constructor.COMP && flagRestriction) {
                obj = valueRestriction.toString();
            }
            if (flagRestriction && inClassRestriction && op != Constructor.COMP) {
                objectsRestriction.add(obj);
            }
            strBGP += " " + obj + " ." + NL;
        }
    }

    public void visit( final RelationConstruction e ) throws AlignmentException {
        op = e.getOperator();
	//System.err.println( "R: < "+sub+", "+op+", "+obj+" >" );
        if (op == Constructor.OR) {
            int size = e.getComponents().size();
            if (valueRestriction != null && !inClassRestriction) {
                obj = valueRestriction.toString();
            }
            String temp = obj;
            for (final PathExpression re : e.getComponents()) {
                strBGP += "{" + NL;
                re.accept(this);
                obj = temp;
                size--;
                if (size != 0) {
                    strBGP += "}" + "UNION " + NL;
                } else {
                    strBGP += "}" + NL;
                }
            }
            objectsRestriction.add(obj);
        } else if ( op == Constructor.AND ) {
            if (valueRestriction != null && !inClassRestriction) {
                obj = valueRestriction.toString();
            }
            String temp = obj;
	    rel = false;
	    List<PathExpression> codomaintotreat = new ArrayList<PathExpression>();
            for ( final PathExpression re : e.getComponents() ) {
		if ( re instanceof RelationCoDomainRestriction ) {
		    codomaintotreat.add( re );
		} else {
		    // This should be ordered...
		    re.accept(this);
		    obj = temp;
		    rel = true;
		}
            }
	    for ( final PathExpression re : codomaintotreat ) {
		re.accept(this);
		obj = temp;
            }
            objectsRestriction.add( obj );
        } else if (op == Constructor.NOT) {
            strBGP += "FILTER (NOT EXISTS {" + NL;
            for (final PathExpression re : e.getComponents()) {
                re.accept(this);
            }
            strBGP += "})" + NL;
        } else if ( op == Constructor.COMP ) {
            int size = e.getComponents().size();
            String temp = sub;
            createVarName();
            for (final PathExpression re : e.getComponents()) {
                re.accept(this);
                size--;
                if (size != 0) {
                    sub = obj;
                    if (size == 1 && valueRestriction != null && !inClassRestriction) {
                        obj = valueRestriction.toString();
                    } else {
                        if (this.getClass() == SPARQLConstructRendererVisitor.class || this.getClass() == SPARQLSelectRendererVisitor.class || this.getClass() == SPARQLLinkkerRendererVisitor.class ) {
                            createVarName();
                        }
                        objectsRestriction.add(obj);
                    }
                }
            }
            sub = temp;
        } else if (op == Constructor.INVERSE) {
            String tempSub = sub;
            for (final PathExpression re : e.getComponents()) {
                String temp = sub;
                sub = obj;
                obj = temp;
                re.accept(this);
                sub = tempSub;
            }
        } else if (op == Constructor.SYMMETRIC) {
            String tempSub = sub;
            for (final PathExpression re : e.getComponents()) {
                strBGP += "{" + NL;
                re.accept(this);
                objectsRestriction.add(obj);
                String temp = sub;
                sub = obj;
                obj = temp;
                strBGP += "} UNION {" + NL;
                re.accept(this);
                objectsRestriction.add(obj);
                strBGP += "}" + NL;
            }
            sub = tempSub;
        } else if (op == Constructor.TRANSITIVE) {
            for (final PathExpression re : e.getComponents()) {
                flagRestriction = true;
                re.accept(this);
                flagRestriction = false;
            }
        } else if (op == Constructor.REFLEXIVE) {
            for (final PathExpression re : e.getComponents()) {
                strBGP += "{" + NL;
                re.accept(this);
                strBGP += "} UNION {" + NL + "FILTER(" + sub + "=" + obj + ")" + NL + "}";
            }
        } else { // JE2014: THIS IS SUPPOSED TO BE A AND!
            if (valueRestriction != null && !inClassRestriction) {
                obj = valueRestriction.toString();
            }
            for ( final PathExpression re : e.getComponents() ) {
                re.accept(this);
                objectsRestriction.add(obj);
                if (weakens && !strBGP.equals("") && !inClassRestriction) {
                    listBGP.add(strBGP);
                    strBGP = "";
                }
            }
        }
        //obj = "?o" + ++count;    	
    }

    public void visit( final RelationCoDomainRestriction c ) throws AlignmentException {
        String stemp = sub;
	if ( !rel ) {
	    strBGP += sub+" "+createPropertyVarName()+" "+obj+" ." + NL;
	    rel = true;
	}
        sub = obj;
        flagRestriction = true;
        c.getCoDomain().accept(this);
        flagRestriction = false;
        sub = stemp;
    }

    public void visit(final RelationDomainRestriction c) throws AlignmentException {
        flagRestriction = true;
        c.getDomain().accept(this);
        flagRestriction = false;
    }

    public void visit( final InstanceId e ) throws AlignmentException {
        if (e.getURI() != null) {
            String id = registerPrefix( e.getURI() );
            if ( flagRestriction ) {
                instance = id;
            } else {
                strBGP += id + " ?p ?o1 ." + NL;
            }
        }
    }

    public void visit( final Value e ) throws AlignmentException {
	if ( e.getType() != null ) uriType = registerPrefix( e.getType() );
        //if ( e.getType() != null ) uriType = decodeDatatype( e.getType().toString() );
	// JE2015: Not sure that this is really useful
        //if ( uriType != null && uriType.equals("") ) uriType = "xsd:string";
        value = e.toRDFString();
    }

    public void visit(final Apply e) throws AlignmentException {
    }

    public void visit(final Aggregate e) throws AlignmentException {
    }

    public void visit(final Transformation transf) throws AlignmentException {
    }

    public void visit( final Datatype e ) throws AlignmentException {
	datatype = decodeDatatype( e.getType() );
    }

    // if known prefix: prefix : suffix
    // Otherwise full uri
    private String decodeDatatype( String datatype ) {
        int index;
        if ( datatype.contains("#")) {
            index = datatype.lastIndexOf("#");
        } else {
            index = datatype.lastIndexOf("/");
        }
        return datatype.substring(index + 1);
    }

}
