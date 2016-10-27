/**
 * 
 */
package de.evoila.cf.broker.service.mysql;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.service.mysql.jdbc.MySQLDbService;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class MySQLCustomImplementation {

	// public void initServiceInstance(ServiceInstance serviceInstance, String[]
	// databases) throws SQLException {
	// String serviceInstanceId = serviceInstance.getId();
	// if (!jdbcService.isConnected()) {
	// ServerAddress host = serviceInstance.getHosts().get(0);
	// jdbcService.createConnection(serviceInstanceId, host.getIp(),
	// host.getPort());
	// }
	// jdbcService.executeUpdate("CREATE ROLE \"" + serviceInstanceId + "\"");
	// for (String database : databases) {
	// jdbcService.executeUpdate("CREATE DATABASE \"" + database + "\" OWNER \""
	// + serviceInstanceId + "\"");
	// }
	// }
	//
	// public void deleteRole(String instanceId) throws SQLException {
	// jdbcService.checkValidUUID(instanceId);
	// jdbcService.executeUpdate("DROP ROLE IF EXISTS \"" + instanceId + "\"");
	// }

	public String bindRoleToDatabase(MySQLDbService jdbcService, String serviceInstanceId, String bindingId)
			throws SQLException {
		SecureRandom random = new SecureRandom();
		String passwd = new BigInteger(130, random).toString(32);

		bindRoleToDatabaseWithPassword(jdbcService, serviceInstanceId, bindingId, passwd);

		return passwd;
	}

	public void bindRoleToDatabaseWithPassword(MySQLDbService jdbcService, String serviceInstanceId, String bindingId,
			String password) throws SQLException {
		jdbcService.executeUpdate("CREATE USER \"" + bindingId + "\" IDENTIFIED BY \"" + password + "\"");
		jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON `" + serviceInstanceId + "`.* TO `" + bindingId + "`@\"%\"");
		jdbcService.executeUpdate("FLUSH PRIVILEGES");
	}

	public void unbindRoleFromDatabase(MySQLDbService jdbcService, String bindingId) throws SQLException {
		jdbcService.executeUpdate("DROP USER \"" + bindingId + "\"");
	}

	public MySQLDbService connection(ServiceInstance serviceInstance) throws SQLException {
		MySQLDbService jdbcService = new MySQLDbService();
		if (jdbcService.isConnected())
			return jdbcService;
		else {
			Assert.notNull(serviceInstance, "ServiceInstance may not be null");
			String instanceId = serviceInstance.getId();
			Assert.notNull(instanceId, "Id of ServiceInstance may not be null");
			ServerAddress host = serviceInstance.getHosts().get(0);
			Assert.notNull(host.getIp(), "Host of ServiceInstance may not be null");
			Assert.notNull(host.getPort(), "Port of ServiceInstance may not be null");
	
			final boolean isConnected = jdbcService.createConnection(host.getIp(),
					host.getPort(), instanceId, instanceId, instanceId);
			if (isConnected)
				return jdbcService;
			else
				return null;
		}
	}
	
	public MySQLDbService connection(String host, int port, String database, String username, String password) throws SQLException {
		MySQLDbService jdbcService = new MySQLDbService();
		if (jdbcService.isConnected())
			return jdbcService;
		else {
			Assert.notNull(host, "Host may not be null");
			Assert.notNull(database, "Database may not be null");
			Assert.notNull(username, "Username may not be null");
			Assert.notNull(password, "Password may not be null");
	
			final boolean isConnected = jdbcService.createConnection(host,
					port, database, username, password);
			if (isConnected)
				return jdbcService;
			else
				return null;
		}
	}

}
