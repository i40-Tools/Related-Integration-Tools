/*
 * $Id: DBServiceImpl.java 2149 2017-07-18 15:12:04Z euzenat $
 *
 * Copyright (C) Seungkeun Lee, 2006
 * Copyright (C) INRIA, 2007-2009, 2013-2017
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

package fr.inrialpes.exmo.align.service;

import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBServiceImpl implements DBService {
    final static Logger logger = LoggerFactory.getLogger( DBServiceImpl.class );

    int id = 0;
    Connection conn = null;
    static String IPAddress = "localhost";
    static String port = "3306";
    static String user = "adminAServ";
    static String database = "AServDB";

    private String options = "autoReconnect=true";
     
    // Used in reconnect()
    static String dbpass = null;
    static String driverString = null;

    // Known values:
    // "org.postgresql.Driver", "jdbc:postgresql"
    // "com.mysql.jdbc.Driver", "jdbc:mysql"
    String driverPrefix;
	
    public DBServiceImpl( String driver, String prefix, String DBPort ) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
	Class.forName(driver).newInstance();
	driverPrefix = prefix;
	port = DBPort;
    }

    public DBServiceImpl( String driver, String prefix, String DBPort, String opt ) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
	this( driver, prefix, DBPort );
	options += opt;
    }

    public String getPrefix() {
	return driverPrefix;
    }

    public void init() {
    }
	 	
    public void connect( String password ) throws SQLException {
	connect( IPAddress, port, user, password, database );
    }
    
    public void connect( String user, String password ) throws SQLException {
	connect( IPAddress, port, user, password, database );
    }
    
    public void connect( String port, String user, String password ) throws SQLException {
	connect( IPAddress, port, user, password, database );
    }
    
    public void connect(String IPAddress, String port, String user, String password ) throws SQLException {
	connect( IPAddress, port, user, password, database );
	}

    public void connect(String IPAddress, String port, String user, String password, String database ) throws SQLException {
	dbpass = password;
	driverString = driverPrefix+"://"+IPAddress+":"+port+"/"+database+"?"+options;
	conn = DriverManager.getConnection( driverString, user, dbpass);
    }

    //with "dbpass" given by "connect"
    public Connection reconnect() throws SQLException {
	conn = DriverManager.getConnection( driverString, user, dbpass);
	return conn;
    }

    public Connection getConnection() throws SQLException {
	if (conn==null || conn.isClosed())
		return reconnect();
	return conn;
    }

    public void close() {
	try {
	    if ( conn != null ) conn.close();
	} catch (Exception ex) {
	    logger.debug( "IGNORED Closing exception", ex );
	}
    }
    
}
