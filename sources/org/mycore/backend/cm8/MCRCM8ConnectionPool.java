/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.backend.cm8;

import java.util.*;

import com.ibm.mm.sdk.common.*;
import com.ibm.mm.sdk.server.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRException;

/**
 * This class handle the connection pool to the IBM Content Manager 8.
 *
 * @author Frank Luetzenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 **/
class MCRCM8ConnectionPool
  {
  private static Vector freeConnections;
  private static Vector usedConnections;
  private static int    maxConnections;
  private static Logger logger=Logger.getLogger("org.mycore.backend.cm8");

  static
    {
    try {
      MCRConfiguration conf = MCRConfiguration.instance();
      PropertyConfigurator.configure(conf.getLoggingProperties());
      maxConnections  = conf.getInt("MCR.persistence_cm8_max_connections",1);
      freeConnections = new Vector(maxConnections);
      usedConnections = new Vector(maxConnections);
      logger.info( "Connecting to ContentManager8: " );
      String servername = conf.getString("MCR.persistence_cm8_library_server");
      String serveruid = conf.getString("MCR.persistence_cm8_user_id");
      String serverpw = conf.getString("MCR.persistence_cm8_password");
      for( int i = 0; i < maxConnections; i++ ) {
        DKDatastoreICM connection = new DKDatastoreICM();
        connection.connect(servername,serveruid,serverpw,"");
//      connection.setOption(DK_OPT_DL_WAKEUPSRV,new Integer(DK_TRUE));
        freeConnections.addElement(connection);
        logger.info( "  one connection created." );
        }
      logger.info( " now are " + maxConnections + " connection(s) created." );
      }
    catch( Exception ex ) {
      throw new MCRException("Error while instanciate CM8 connection pool.",
        ex ); }
    }
  
  /**
   * This methode returns a connection to the CM8.
   *
   * @return a connected datastore
   **/
  static synchronized DKDatastoreICM getConnection()
    {
    if( usedConnections.size() == maxConnections ) {
      try{ MCRCM8ConnectionPool.class.wait(); }
      catch( InterruptedException ex ){}
      return getConnection();
      }
    else {
      DKDatastoreICM connection = (DKDatastoreICM)
        (freeConnections.firstElement());
      freeConnections.removeElement( connection );
      usedConnections.addElement( connection );
      return connection;
      }
    }

  /**
   * This methode release a given datastore connection.
   *
   * @param connection            the datastor connection
   **/
  static synchronized void releaseConnection( DKDatastoreICM connection )
    {
    if((!freeConnections.contains(connection))
      && (usedConnections.contains(connection))) {
      usedConnections.removeElement( connection );
      freeConnections.addElement   ( connection );
      MCRCM8ConnectionPool.class.notifyAll();
      }
    }

  /**
   * The method return the logger for org.mycore.backend.cm8 .
   *
   * @return the logger.
   **/
  static final Logger getLogger()
    { return logger; }

  }

