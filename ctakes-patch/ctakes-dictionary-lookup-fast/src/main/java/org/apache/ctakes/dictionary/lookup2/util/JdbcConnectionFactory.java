package org.apache.ctakes.dictionary.lookup2.util;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Some JDBC Connections can be reused, for instance by a Dictionary and Concept Factory.
 * This Singleton keeps a map of JDBC URLs to open and reusable Connections
 *
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/29/2014
 */
public enum JdbcConnectionFactory {
   INSTANCE;

   static public final String JDBC_DRIVER = "jdbcDriver";
   static public final String JDBC_URL = "jdbcUrl";
   static public final String JDBC_USER = "jdbcUser";
   static public final String JDBC_PASS = "jdbcPass";

   static final private Logger LOGGER = Logger.getLogger( "JdbcConnectionFactory" );
   static final private Logger DOT_LOGGER = Logger.getLogger( "ProgressAppender" );
   static final private Logger EOL_LOGGER = Logger.getLogger( "ProgressDone" );

   static private final String HSQL_FILE_PREFIX = "jdbc:hsqldb:file:";
   static private final String HSQL_DB_EXT = ".script";
   private final Map<String, Connection> CONNECTIONS = Collections.synchronizedMap( new HashMap<String, Connection>() );

   public static JdbcConnectionFactory getInstance() {
      return INSTANCE;
   }

   /**
    * Get an existing Connection or create and store a new one
    *
    * @param jdbcDriver -
    * @param jdbcUrl    -
    * @param jdbcUser   -
    * @param jdbcPass   -
    * @return a previously opened or new Connection
    * @throws SQLException if a JDBC Driver could not be created or registered,
    *                      or if a Connection could not be made to the given <code>jdbcUrl</code>
    */
   public Connection getConnection( final String jdbcDriver,
                                    final String jdbcUrl,
                                    final String jdbcUser,
                                    final String jdbcPass ) throws SQLException {
      Connection connection = CONNECTIONS.get( jdbcUrl );
      if ( connection != null ) {
         return connection;
      }
      String trueJdbcUrl = jdbcUrl;
      if ( jdbcUrl.startsWith( HSQL_FILE_PREFIX ) ) {
         // Hack for hsqldb file needing to be absolute or relative to current working directory
         trueJdbcUrl = HSQL_FILE_PREFIX + getConnectionUrl( jdbcUrl );
      }
      try {
         // DO NOT use try with resources here.
         // Try with resources uses a closable and closes it when exiting the try block
         final Driver driver = (Driver)Class.forName( jdbcDriver ).newInstance();
         DriverManager.registerDriver( driver );
      } catch ( SQLException sqlE ) {
         LOGGER.error( "Could not register Driver " + jdbcDriver, sqlE );
         throw sqlE;
      } catch ( ClassNotFoundException | InstantiationException | IllegalAccessException multE ) {
         LOGGER.error( "Could not create Driver " + jdbcDriver, multE );
         throw new SQLException( multE );
      }
      LOGGER.info( "Connecting to " + jdbcUrl + ":" );
      final Timer timer = new Timer();
      timer.scheduleAtFixedRate( new DotPlotter(), 333, 333 );
      try {
         // DO NOT use try with resources here.
         // Try with resources uses a closable and closes it when exiting the try block
         // We need the Connection later, and if it is closed then it is useless
         connection = DriverManager.getConnection( trueJdbcUrl, jdbcUser, jdbcPass );
      } catch ( SQLException sqlE ) {
         timer.cancel();
         EOL_LOGGER.error( "" );
         LOGGER.error( "  Could not create Connection with " + trueJdbcUrl + " as " + jdbcUser, sqlE );
         throw sqlE;
      }
      timer.cancel();
      EOL_LOGGER.info( "" );
      LOGGER.info( " Database connected" );
      CONNECTIONS.put( jdbcUrl, connection );
      return connection;
   }

   /**
    * Uses {@link org.apache.ctakes.core.resource.FileLocator} to get the canonical path to the database file
    *
    * @param jdbcUrl -
    * @return -
    * @throws SQLException
    */
   static private String getConnectionUrl( final String jdbcUrl ) throws SQLException {
      LOGGER.info("jdbc url" + jdbcUrl);
      final String urlDbPath = jdbcUrl.substring( HSQL_FILE_PREFIX.length() );
      final String urlFilePath = urlDbPath + HSQL_DB_EXT;
      try {
         final String fullPath = FileLocator.getFullPath( urlFilePath );
         return fullPath.substring( 0, fullPath.length() - HSQL_DB_EXT.length() );
      } catch ( FileNotFoundException fnfE ) {
         throw new SQLException( "No Hsql DB exists at Url", fnfE );
      }
   }

   static private class DotPlotter extends TimerTask {
      private int _count = 0;

      @Override
      public void run() {
         DOT_LOGGER.info( "." );
         _count++;
         if ( _count % 50 == 0 ) {
            EOL_LOGGER.info( " " + _count );
         }
      }
   }

}
