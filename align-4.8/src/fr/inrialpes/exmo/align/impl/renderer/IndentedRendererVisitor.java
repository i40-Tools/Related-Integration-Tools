/*
 * $Id: IndentedRendererVisitor.java 2086 2015-10-23 08:21:09Z euzenat $
 *
 * Copyright (C) INRIA, 2010, 2012, 2015
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

/**
 * Structure for indented rendering
 *
 * @author Jérôme Euzenat
 * @version $Id: IndentedRendererVisitor.java 2086 2015-10-23 08:21:09Z euzenat $
 */

public class IndentedRendererVisitor extends GenericReflectiveVisitor {

    PrintWriter writer = null;

    protected String INDENT = "  ";

    protected String NL = "";

    /** String for the pretty linebreak. **/
    protected String linePrefix = "";

    private int prefixCount = 0;

    public IndentedRendererVisitor( PrintWriter writer ){
	NL = System.getProperty("line.separator");
	this.writer = writer;
    }

    public void setIndentString( String ind ) {
	INDENT = ind;
    }

    public void setNewLineString( String nl) { 
	NL = nl;
    }

    // ===================================================================
    // pretty printing management
    // JE: I THINK THAT THIS IS CONVENIENT BUT INDUCES A SERIOUS LAG IN
    // PERFORMANCES (BOTH VERSIONS v1 and v2)
    // LET SEE IF THERE IS NO WAY TO DO THIS DIRECTLY IN THE WRITER  !!!

    /**
     * Increases the lineprefix by one INDENT
     */
    protected void increaseIndent() {
	prefixCount++;
    }
    
    /**
     * Decreases the lineprefix by one INDENT
     */
    protected void decreaseIndent() {
	if (prefixCount > 0) {
	    prefixCount--;
	}
    }
    
    // JE: I would like to see benchmarks showing that this is more efficient
    // than adding them to buffer directly each time
    private void calcPrefix() {
	StringBuilder buffer = new StringBuilder();
	buffer.append(NL);
	for (int i = 0; i < prefixCount; i++) {
	    buffer.append(INDENT);
	}
	linePrefix = buffer.toString();
    }

    protected void indentedOutputln( String s ){
	for (int i = 0; i < prefixCount; i++) writer.print(INDENT);
	writer.print(s);
	writer.print(NL);
    }
    protected void indentedOutput( String s ){
	for (int i = 0; i < prefixCount; i++) writer.print(INDENT);
	writer.print(s);
    }
    protected void indentedOutputln(){
	for (int i = 0; i < prefixCount; i++) writer.print(INDENT);
	writer.print(NL);
    }
    protected void indentedOutput(){
	for (int i = 0; i < prefixCount; i++) writer.print(INDENT);
    }
    protected void outputln( String s ){
	writer.print(s);
	writer.print(NL);
    }
    protected void output( String s ){
	writer.print(s);
    }
    protected void outputln(){
	writer.print(NL);
    }
}
