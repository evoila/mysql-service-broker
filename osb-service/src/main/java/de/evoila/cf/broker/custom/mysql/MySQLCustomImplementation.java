/**
 * 
 */
package de.evoila.cf.broker.custom.mysql;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.cpi.existing.CustomExistingService;
import de.evoila.cf.cpi.existing.CustomExistingServiceConnection;
import de.evoila.de.evoila.cf.cpi.existing.MySQLExistingServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class MySQLCustomImplementation implements CustomExistingService {

    private Logger log = LoggerFactory.getLogger(MySQLCustomImplementation.class);

	@Autowired
	private ServiceDefinitionRepository serviceDefinitionRepository;
	
	@Autowired(required=false)
	private MySQLExistingServiceFactory existingServiceFactory;

    @Autowired(required = false)
    private ExistingEndpointBean existingEndpointBean;

	public void bindRoleToDatabase(MySQLDbService jdbcService, String username,
			String password, String database) throws SQLException {
		jdbcService.executeUpdate("CREATE USER \"" + username + "\" IDENTIFIED BY \"" + password + "\"");
		jdbcService.executeUpdate("GRANT ALL PRIVILEGES ON `" + database + "`.* TO `" + username + "`@\"%\"");
		jdbcService.executeUpdate("FLUSH PRIVILEGES");
	}

	public void unbindRoleFromDatabase(MySQLDbService jdbcService, String username) throws SQLException {
		jdbcService.executeUpdate("DROP USER \"" + username + "\"");
	}

	public MySQLDbService connection(ServiceInstance serviceInstance, Plan plan) throws SQLException {
		MySQLDbService jdbcService = new MySQLDbService();
		if (jdbcService.isConnected())
			return jdbcService;
		else {

            if(plan.getPlatform() == Platform.BOSH)
                jdbcService.createConnection(serviceInstance.getUsername(), serviceInstance.getPassword(),
                        "admin", serviceInstance.getHosts());
            else if (plan.getPlatform() == Platform.EXISTING_SERVICE)
                jdbcService.createConnection(existingEndpointBean.getUsername(), existingEndpointBean.getPassword(),
                        existingEndpointBean.getDatabase(), existingEndpointBean.getHostsWithServerAddress());
            return  jdbcService;
		}
	}

	public CustomExistingServiceConnection connection(List<String> hosts, int port, String database, String username, String password) throws SQLException {
		MySQLDbService jdbcService = new MySQLDbService();

        List<ServerAddress> serverAddresses = new ArrayList<>();
        for (String address : hosts) {
            serverAddresses.add(new ServerAddress("", address, port));
            log.info("Opening connection to " + address + ":" + port);
        }

        jdbcService.createConnection(username, password, database, serverAddresses);
        return jdbcService;
	}

	@Override
	public void bindRoleToInstanceWithPassword(CustomExistingServiceConnection connection, String database,
			String username, String password) throws Exception {
		if(connection instanceof MySQLDbService)
			this.bindRoleToDatabase((MySQLDbService) connection, username, password, database);
	}

}
