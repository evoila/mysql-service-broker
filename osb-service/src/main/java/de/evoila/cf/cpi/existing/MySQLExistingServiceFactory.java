/**
 * 
 */
package de.evoila.cf.cpi.existing;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.custom.mysql.MySQLCustomImplementation;
import de.evoila.cf.broker.custom.mysql.MySQLDbService;
import de.evoila.cf.broker.custom.mysql.MySQLUtils;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.broker.util.RandomString;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author Christian Brinker, evoila.
 *
 */
@Service
@ConditionalOnBean(ExistingEndpointBean.class)
public class MySQLExistingServiceFactory extends ExistingServiceFactory {

    RandomString usernameRandomString = new RandomString(10);
    RandomString passwordRandomString = new RandomString(15);

	private MySQLCustomImplementation mySQLCustomImplementation;

    private ExistingEndpointBean existingEndpointBean;

    public MySQLExistingServiceFactory(PlatformRepository platformRepository, ServicePortAvailabilityVerifier portAvailabilityVerifier, ExistingEndpointBean existingEndpointBean,
                                       MySQLCustomImplementation mySQLCustomImplementation) {
        super(platformRepository, portAvailabilityVerifier, existingEndpointBean);
        this.mySQLCustomImplementation = mySQLCustomImplementation;
        this.existingEndpointBean = existingEndpointBean;
    }

    public void createDatabase(MySQLDbService connection, String database) throws PlatformException {
		try {
			connection.executeUpdate("CREATE DATABASE `" + database + "`");
		} catch (SQLException e) {
			log.error(e.toString());
			throw new PlatformException("Could not add to database");
		}
	}

	public void deleteDatabase(MySQLDbService connection, String database) throws PlatformException {
		try {
			connection.executeUpdate("DROP DATABASE `" + database + "`");
		} catch (SQLException e) {
			log.error(e.toString());
			throw new PlatformException("Could not remove from database");
		}
	}

	@Override
    public void deleteInstance(ServiceInstance serviceInstance, Plan plan) throws PlatformException {
        MySQLDbService mySQLDbService = this.connection(serviceInstance, plan);

        deleteDatabase(mySQLDbService, MySQLUtils.dbName(serviceInstance.getId()));
	}

	@Override
    public ServiceInstance createInstance(ServiceInstance serviceInstance, Plan plan, Map<String, Object> parameters) throws PlatformException {
        String username = usernameRandomString.nextString();
        String password = passwordRandomString.nextString();

        serviceInstance.setUsername(username);
        serviceInstance.setPassword(password);

        MySQLDbService mySQLDbService = this.connection(serviceInstance, plan);

        createDatabase(mySQLDbService, MySQLUtils.dbName(serviceInstance.getId()));

        return serviceInstance;
	}

    private MySQLDbService connection(ServiceInstance serviceInstance, Plan plan) {
        MySQLDbService jdbcService = new MySQLDbService();

        if (plan.getPlatform() == Platform.EXISTING_SERVICE)
            jdbcService.createConnection(existingEndpointBean.getUsername(), existingEndpointBean.getPassword(),
                    existingEndpointBean.getDatabase(), existingEndpointBean.getHosts());

        return jdbcService;
    }
}
