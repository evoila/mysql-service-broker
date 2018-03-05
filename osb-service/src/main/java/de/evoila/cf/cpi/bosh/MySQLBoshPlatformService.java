package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.exception.PlatformException;
import de.evoila.cf.broker.model.DashboardClient;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.repository.PlatformRepository;
import de.evoila.cf.broker.service.CatalogService;
import de.evoila.cf.broker.service.availability.ServicePortAvailabilityVerifier;
import io.bosh.client.deployments.Deployment;
import io.bosh.client.errands.ErrandSummary;
import io.bosh.client.vms.Vm;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by reneschollmeyer, evoila on 28.02.18.
 */
@Service
@ConditionalOnBean(BoshProperties.class)
public class MySQLBoshPlatformService extends BoshPlatformService {
    private static final int defaultPort = 3306;

    public MySQLBoshPlatformService(PlatformRepository repository, CatalogService catalogService, ServicePortAvailabilityVerifier availabilityVerifier, BoshProperties boshProperties, Optional<DashboardClient> dashboardClient) {
        super(repository, catalogService, availabilityVerifier, boshProperties, dashboardClient, new MySQLDeploymentManager(boshProperties));
    }

    public void runCreateErrands(ServiceInstance instance, Plan plan, Deployment deployment, Observable<List<ErrandSummary>> errands) throws PlatformException {  }

    protected void runUpdateErrands(ServiceInstance instance, Plan plan, Deployment deployment, Observable<List<ErrandSummary>> errands) throws PlatformException {  }

    protected void runDeleteErrands(ServiceInstance instance, Deployment deployment, Observable<List<ErrandSummary>> errands) { }

    @Override
    protected void updateHosts(ServiceInstance instance, Plan plan, Deployment deployment) {
        final int port;
        if(plan.getMetadata().containsKey(MySQLDeploymentManager.PORT)) {
            port = (int) plan.getMetadata().get(MySQLDeploymentManager.PORT);
        } else {
            port = defaultPort;
        }

        List<Vm> vms = connection.connection().vms().listDetails(BoshPlatformService.DEPLOYMENT_NAME_PREFIX + instance.getId()).toBlocking().first();
        if(instance.getHosts() == null) {
            instance.setHosts(new ArrayList<>());
        }

        instance.getHosts().clear();

        vms.forEach(vm -> {
            instance.getHosts().add(new ServerAddress("Host-" + vm.getIndex(), vm.getIps().get(0), port));
        });
    }
}
