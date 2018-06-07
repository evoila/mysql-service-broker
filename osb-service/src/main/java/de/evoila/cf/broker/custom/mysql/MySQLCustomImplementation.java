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
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import de.evoila.cf.cpi.existing.MySQLExistingServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * @author Johannes Hiemer.
 *
 */
@Service
public class MySQLCustomImplementation {

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

    public MySQLDbService connection(ServiceInstance serviceInstance, Plan plan) {
        MySQLDbService jdbcService = new MySQLDbService();

        if(plan.getPlatform() == Platform.BOSH) {
            List<ServerAddress> serverAddresses = serviceInstance.getHosts();

            if (plan.getMetadata().getIngressInstanceGroup() != null &&
                    plan.getMetadata().getIngressInstanceGroup().length() > 0)
                serverAddresses = ServiceInstanceUtils.filteredServerAddress(serviceInstance.getHosts(),
                        plan.getMetadata().getIngressInstanceGroup());

            jdbcService.createConnection(serviceInstance.getUsername(), serviceInstance.getPassword(),
                    MySQLUtils.dbName(serviceInstance.getId()), serverAddresses);
        } else if (plan.getPlatform() == Platform.EXISTING_SERVICE)
            jdbcService.createConnection(existingEndpointBean.getUsername(), existingEndpointBean.getPassword(),
                    existingEndpointBean.getDatabase(), serviceInstance.getHosts());

        return jdbcService;
    }

}
