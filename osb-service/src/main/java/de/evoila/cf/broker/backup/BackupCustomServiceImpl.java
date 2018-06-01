package de.evoila.cf.broker.backup;

import de.evoila.cf.broker.bean.BackupTypeConfiguration;
import de.evoila.cf.broker.custom.mysql.MySQLUtils;
import de.evoila.cf.broker.exception.ServiceInstanceDoesNotExistException;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.ServiceInstanceRepository;
import de.evoila.cf.broker.service.BackupCustomService;
import de.evoila.cf.model.DatabaseCredential;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(BackupTypeConfiguration.class)
public class BackupCustomServiceImpl implements BackupCustomService {

    BackupTypeConfiguration backupTypeConfiguration;

    ServiceInstanceRepository serviceInstanceRepository;

    public BackupCustomServiceImpl(BackupTypeConfiguration backupTypeConfiguration,
                                   ServiceInstanceRepository serviceInstanceRepository) {
        this.backupTypeConfiguration = backupTypeConfiguration;
        this.serviceInstanceRepository = serviceInstanceRepository;
    }

    public DatabaseCredential getCredentialsForInstanceId(String serviceInstanceId) throws ServiceInstanceDoesNotExistException {
        ServiceInstance instance = serviceInstanceRepository.getServiceInstance(serviceInstanceId);

        if(instance == null || instance.getHosts().size() <= 0) {
            throw new ServiceInstanceDoesNotExistException(serviceInstanceId);
        }

        DatabaseCredential credential = new DatabaseCredential();
        credential.setContext(MySQLUtils.dbName(instance.getId()));
        credential.setUsername(instance.getUsername());
        credential.setPassword(instance.getPassword());
        credential.setHostname(instance.getHosts().get(0).getIp());
        credential.setPort(instance.getHosts().get(0).getPort());
        credential.setType(backupTypeConfiguration.getType());

        return credential;
    }

}
