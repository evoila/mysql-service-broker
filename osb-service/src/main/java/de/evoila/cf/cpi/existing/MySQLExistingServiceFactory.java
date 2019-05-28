/**
 * 
 */
package de.evoila.cf.cpi.existing;

import de.evoila.cf.broker.bean.ExistingEndpointBean;
import de.evoila.cf.broker.custom.mysql.MySQLCustomImplementation;
import de.evoila.cf.broker.custom.mysql.MySQLDbService;
import de.evoila.cf.broker.custom.mysql.MySQLUtils;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.security.credentials.CredentialStore;
import de.evoila.cf.security.credentials.DefaultCredentialConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author Christian Brinker, Johannes Hiemer.
 */
@Service
@ConditionalOnBean(ExistingEndpointBean.class)
public class MySQLExistingServiceFactory extends ExistingServiceFactory {

    private ExistingEndpointBean existingEndpointBean;

    private MySQLCustomImplementation mysqlCustomImplementation;

    private CredentialStore credentialStore;

    public MySQLExistingServiceFactory(PlatformRepository platformRepository,
                                       MySQLCustomImplementation mySQLCustomImplementation,
                                       ServicePortAvailabilityVerifier portAvailabilityVerifier,
                                       ExistingEndpointBean existingEndpointBean,
                                       CredentialStore credentialStore) {
        super(platformRepository, portAvailabilityVerifier, existingEndpointBean);
        this.mysqlCustomImplementation = mySQLCustomImplementation;
        this.existingEndpointBean = existingEndpointBean;
        this.credentialStore = credentialStore;
    }

	@Override
    public void deleteInstance(ServiceInstance serviceInstance, Plan plan) throws PlatformException {
        MySQLDbService mySQLDbService = mysqlCustomImplementation.connection(serviceInstance, plan, null, "mysql");

        mysqlCustomImplementation.deleteDatabase(mySQLDbService, MySQLUtils.dbName(serviceInstance.getId()));

        credentialStore.deleteCredentials(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
        credentialStore.deleteCredentials(serviceInstance, DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS);
        credentialStore.deleteCredentials(serviceInstance, DefaultCredentialConstants.BACKUP_CREDENTIALS);
	}

    @Override
    public ServiceInstance getInstance(ServiceInstance serviceInstance, Plan plan) {
        return serviceInstance;
    }

    @Override
    public ServiceInstance createInstance(ServiceInstance serviceInstance, Plan plan, Map<String, Object> parameters) throws PlatformException {
        if (existingEndpointBean.getBackupCredentials() != null)
            credentialStore.createUser(serviceInstance, DefaultCredentialConstants.BACKUP_AGENT_CREDENTIALS,
                    existingEndpointBean.getBackupCredentials().getUsername(), existingEndpointBean.getBackupCredentials().getPassword());

        credentialStore.createUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
        UsernamePasswordCredential serviceInstanceUsernamePasswordCredential = credentialStore.getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);

        credentialStore.createUser(serviceInstance, DefaultCredentialConstants.BACKUP_CREDENTIALS, serviceInstanceUsernamePasswordCredential.getUsername(),
                serviceInstanceUsernamePasswordCredential.getPassword());

        serviceInstance.setUsername(serviceInstanceUsernamePasswordCredential.getUsername());
        serviceInstance.setHosts(existingEndpointBean.getHosts());
        MySQLDbService mySQLDbService = mysqlCustomImplementation.connection(serviceInstance, plan,
                new UsernamePasswordCredential(existingEndpointBean.getUsername(), existingEndpointBean.getPassword()), "mysql");

        String database = MySQLUtils.dbName(serviceInstance.getId());
        mysqlCustomImplementation.createDatabase(mySQLDbService, database);
        try {
            mysqlCustomImplementation.bindRoleToDatabase(mySQLDbService, serviceInstanceUsernamePasswordCredential.getUsername(),
                    serviceInstanceUsernamePasswordCredential.getPassword(), database);
        } catch(SQLException ex) {
            throw new PlatformException(ex);
        }


        return serviceInstance;
	}

}
