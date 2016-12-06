/**
 * 
 */
package de.evoila.cf.broker.service.custom;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.service.mysql.MySQLCustomImplementation;
import de.evoila.cf.broker.service.mysql.jdbc.MySQLDbService;
import de.evoila.cf.cpi.existing.ExistingServiceFactory;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Service
@ConditionalOnProperty(prefix="existing.endpoint", name={"host","port","username","password","database"},havingValue="")
public class MySQLExistingServiceFactory extends ExistingServiceFactory {
	
	@Value("${existing.endpoint.host}")
	private String host;
	
	@Value("${existing.endpoint.port}")
	private int port;
	
	@Value("${existing.endpoint.username}")
	private String username;
	
	@Value("${existing.endpoint.password}")
	private String password;
	
	@Value("${existing.endpoint.database}")
	private String database;
	
	@Autowired
	private MySQLCustomImplementation mysql;
	
	/* (non-Javadoc)
	 * @see de.evoila.cf.cpi.existing.ExistingServiceFactory#getExistingServiceHosts()
	 */
	@Override
	protected List<ServerAddress> getExistingServiceHosts() {
		ServerAddress serverAddress = new ServerAddress("existing_cluster", host, port);
		return Lists.newArrayList(serverAddress);
	}

	@Override
	public void deleteServiceInstance(ServiceInstance serviceInstance) throws PlatformException {
		try {
			MySQLDbService connection = mysql.connection(host, port, database, username, password);
			
			String instanceId = serviceInstance.getId();
			deleteDatabase(connection, instanceId);
		} catch (SQLException e) {
			log.error(e.toString());
			throw new PlatformException("Could not delete service instance in existing database server", e);
		}
	}

	@Override
	protected void provisionServiceInstance(ServiceInstance serviceInstance, Plan plan,
			Map<String, String> customProperties) throws PlatformException {
		try {
			MySQLDbService connection = mysql.connection(host, port, database, username, password);
			
			String instanceId = serviceInstance.getId();
			createDatabase(connection, instanceId);
			mysql.bindRoleToDatabaseWithPassword(connection, instanceId, instanceId, instanceId);
		} catch (SQLException e) {
			log.error(e.toString());
			throw new PlatformException("Could not create service instance in existing database server", e);
		}
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

	public String getPassword() {
		return this.password;
	}
}
