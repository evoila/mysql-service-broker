package de.evoila.cf.broker.backup;

import de.evoila.cf.broker.custom.mysql.MySQLCustomImplementation;
import de.evoila.cf.broker.custom.mysql.MySQLDbService;
import de.evoila.cf.broker.custom.mysql.MySQLUtils;
import de.evoila.cf.broker.exception.ServiceBrokerException;
import de.evoila.cf.broker.exception.ServiceDefinitionDoesNotExistException;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.Platform;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.model.catalog.plan.Plan;
import de.evoila.cf.broker.model.credential.UsernamePasswordCredential;
import de.evoila.cf.broker.repository.ServiceDefinitionRepository;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.service.BackupCustomService;
import de.evoila.cf.cpi.CredentialConstants;
import de.evoila.cf.security.credentials.CredentialStore;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Johannes Hiemer.
 */
@Service
public class BackupCustomServiceImpl implements BackupCustomService {

    private ServiceInstanceRepository serviceInstanceRepository;

    private MySQLCustomImplementation mysqlCustomImplementation;

    private ServiceDefinitionRepository serviceDefinitionRepository;

    private CredentialStore credentialStore;

    public BackupCustomServiceImpl(ServiceInstanceRepository serviceInstanceRepository,
                                   MySQLCustomImplementation mysqlCustomImplementation,
                                   ServiceDefinitionRepository serviceDefinitionRepository,
                                   CredentialStore credentialStore) {
        this.serviceInstanceRepository = serviceInstanceRepository;
        this.mysqlCustomImplementation = mysqlCustomImplementation;
        this.serviceDefinitionRepository = serviceDefinitionRepository;
        this.credentialStore = credentialStore;
    }

    @Override
    public Map<String, String> getItems(String serviceInstanceId) throws ServiceInstanceDoesNotExistException,
            ServiceDefinitionDoesNotExistException {
        ServiceInstance serviceInstance = serviceInstanceRepository.getServiceInstance(serviceInstanceId);

        if(serviceInstance == null || serviceInstance.getHosts().size() <= 0) {
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }

        Plan plan = serviceDefinitionRepository.getPlan(serviceInstance.getPlanId());

        Map<String, String> result = new HashMap<>();
        if (plan.getPlatform().equals(Platform.BOSH)) {
            UsernamePasswordCredential usernamePasswordCredential = credentialStore.getUser(serviceInstance, CredentialConstants.ROOT_CREDENTIALS);
            MySQLDbService mySQLDbService = mysqlCustomImplementation.connection(serviceInstance, plan, usernamePasswordCredential);

            try {
                Map<String, String> databases = mySQLDbService.executeSelect("SHOW DATABASES");

                for(Map.Entry<String, String> database : databases.entrySet())
                    result.put(database.getValue(), database.getValue());
            } catch(SQLException ex) {
                new ServiceBrokerException("Could not load databases", ex);
            }
        } else if (plan.getPlatform().equals(Platform.EXISTING_SERVICE)) {
            result.put(MySQLUtils.dbName(serviceInstance.getId()), MySQLUtils.dbName(serviceInstance.getId()));
        }

        return result;
    }

}
