/**
 * 
 */
package de.evoila.cf.broker.service.mysql;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.broker.service.custom.MySQLExistingServiceFactory;
import de.evoila.cf.broker.service.mysql.jdbc.MySQLDbService;
import de.evoila.cf.cpi.existing.CustomExistingService;
import de.evoila.cf.cpi.existing.CustomExistingServiceConnection;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class MySQLCustomImplementation implements CustomExistingService {

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
	
	@Autowired
	private ServiceDefinitionRepository serviceDefinitionRepository;
	
	@Autowired(required=false)
	private MySQLExistingServiceFactory existingServiceFactory;

	public String bindRoleToDatabase(MySQLDbService jdbcService, String serviceInstanceId, String bindingId)
			throws SQLException {
		SecureRandom random = new SecureRandom();
		String passwd = new BigInteger(130, random).toString(32);

		bindRoleToDatabaseWithPassword(jdbcService, serviceInstanceId, bindingId, passwd);

		return passwd;
	}

	public void bindRoleToDatabaseWithPassword(MySQLDbService jdbcService, String database, String username,
			String password) throws SQLException {
		jdbcService.executeUpdate("CREATE USER \"" + username + "\" IDENTIFIED BY \"" + password + "\"");
		jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON `" + database + "`.* TO `" + username + "`@\"%\"");
		jdbcService.executeUpdate("FLUSH PRIVILEGES");
	}

	public void unbindRoleFromDatabase(MySQLDbService jdbcService, String bindingId) throws SQLException {
		jdbcService.executeUpdate("DROP USER \"" + bindingId + "\"");
	}

	public MySQLDbService connection(ServiceInstance serviceInstance) throws SQLException, ServiceBrokerException {
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
			
			String password = instanceId;
			String planId = serviceInstance.getPlanId();
			Plan plan = serviceDefinitionRepository.getPlan(planId);
			if(plan.getPlatform() == Platform.EXISTING_SERVICE) {
				password = existingServiceFactory.getPassword();
			}
	
			final boolean isConnected = jdbcService.createConnection(host.getIp(),
					host.getPort(), instanceId, null, password);
			if (isConnected)
				return jdbcService;
			else
				return null;
		}
	}
	
	public CustomExistingServiceConnection connection(List<String> hosts, int port, String database, String username, String password) throws SQLException {
		MySQLDbService jdbcService = new MySQLDbService();
		if (jdbcService.isConnected())
			return jdbcService;
		else {
			Assert.notNull(hosts, "Host may not be null");
			Assert.notNull(database, "Database may not be null");
			Assert.notNull(username, "Username may not be null");
			Assert.notNull(password, "Password may not be null");
	
			String host = hosts.get(0);
			final boolean isConnected = jdbcService.createConnection(host,
					port, database, username, password);
			if (isConnected)
				return jdbcService;
			else
				return null;
		}
	}

	@Override
	public void bindRoleToInstanceWithPassword(CustomExistingServiceConnection connection, String database,
			String username, String password) throws Exception {
		if(connection instanceof MySQLDbService)
			this.bindRoleToDatabaseWithPassword((MySQLDbService) connection, database, username, password);		
	}

}
