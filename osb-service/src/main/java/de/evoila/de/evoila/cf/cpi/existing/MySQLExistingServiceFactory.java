/**
 * 
 */
package de.evoila.de.evoila.cf.cpi.existing;

import de.evoila.cf.broker.custom.mysql.MySQLDbService;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.custom.mysql.MySQLCustomImplementation;
import de.evoila.cf.cpi.existing.CustomExistingService;
import de.evoila.cf.cpi.existing.CustomExistingServiceConnection;
import de.evoila.cf.cpi.existing.ExistingServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Service
@ConditionalOnProperty(prefix="existing.endpoint", name={"port","username","password","database"},havingValue="")
public class MySQLExistingServiceFactory extends ExistingServiceFactory {
	
	@Autowired
	private MySQLCustomImplementation mySQLCustomImplementation;
	
	protected CustomExistingService getCustomExistingService() {
		return mySQLCustomImplementation;
	}

	public void createDatabase(MySQLDbService connection, String database) throws PlatformException {
		try {
			connection.executeUpdate("CREATE DATABASE `" + database + "`");
			//connection.executeUpdate("REVOKE all on database " + database + " from public");
		} catch (SQLException e) {
			log.error(e.toString());
			throw new PlatformException("Could not add to database");
		}
	}

	public void deleteDatabase(MySQLDbService connection, String database) throws PlatformException {
		try {
			//connection.executeUpdate("REVOKE all on database \"" + database + "\" from public");
			connection.executeUpdate("DROP DATABASE `" + database + "`");
		} catch (SQLException e) {
			log.error(e.toString());
			throw new PlatformException("Could not remove from database");
		}
	}

	@Override
	protected void deleteInstance(CustomExistingServiceConnection connection, String instanceId) throws PlatformException {
		if(connection instanceof MySQLDbService)
			deleteDatabase((MySQLDbService) connection, instanceId);
	}

	@Override
	protected void createInstance(CustomExistingServiceConnection connection, String instanceId) throws PlatformException {
		if(connection instanceof MySQLDbService)
			createDatabase((MySQLDbService) connection, instanceId);
	}
}
