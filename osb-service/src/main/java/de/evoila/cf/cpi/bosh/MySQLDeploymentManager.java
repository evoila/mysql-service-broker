package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 28.02.18.
 */
public class MySQLDeploymentManager extends DeploymentManager {
    public static final String INSTANCE_GROUP = "mariadb";
    public static final String DATA_PATH = "data_path";
    public static final String PORT = "port";
    public static final String MYSQLD_EXPORTER_PASSWORD = "password";
    public static final String MYSQL_ADMIN_PASSWORD = "admin_password";
    public static final String MYSQL_CLUSTER_HEALTH_PASSWORD = "password";
    public static final String GALERA_ENDPOINT_PASSWORD = "endpoint_password";
    public static final String GALERA_DB_PASSWORD = "db_password";

    public MySQLDeploymentManager(BoshProperties properties) {
        super(properties);
    }

    @Override
    protected void replaceParameters(ServiceInstance instance, Manifest manifest, Plan plan, Map<String, String> customParameters) {
        HashMap<String, Object> properties = new HashMap<>();
        properties.putAll(plan.getMetadata());
        properties.putAll(customParameters);

        Map<String, Object> manifestProperties = manifest.getInstance_groups()
                .stream()
                .filter(i -> i.getName().equals(INSTANCE_GROUP))
                .findAny().get().getProperties();

        HashMap<String, Object> mysqldExporter = (HashMap<String, Object>) manifestProperties.get("mysqld_exporter");
        HashMap<String, Object> mysql = (HashMap<String, Object>) manifestProperties.get("mysql");
        HashMap<String, Object> clusterHealth = (HashMap<String, Object>) mysql.get("cluster_health");
        HashMap<String, Object> galeraHealthcheck = (HashMap<String, Object>) mysql.get("galera_healthcheck");

        if(clusterHealth == null) {
            clusterHealth = new HashMap<>();
            mysql.put("cluster_health", clusterHealth);
        }

        if(galeraHealthcheck == null) {
            galeraHealthcheck = new HashMap<>();
            mysql.put("galera_healthcheck", galeraHealthcheck);
        }

        instance.setUsername((String) mysql.get("admin_username"));
        instance.setPassword(instance.getInternalId());

        mysqldExporter.put(MYSQLD_EXPORTER_PASSWORD, instance.getInternalId());
        mysql.put(MYSQL_ADMIN_PASSWORD, instance.getInternalId());
        clusterHealth.put(MYSQL_CLUSTER_HEALTH_PASSWORD, instance.getInternalId());
        galeraHealthcheck.put(GALERA_ENDPOINT_PASSWORD, instance.getInternalId());
        galeraHealthcheck.put(GALERA_DB_PASSWORD, instance.getInternalId());

        if(properties.containsKey(DATA_PATH)) {
            mysql.put(DATA_PATH, properties.get(DATA_PATH));
        }

        if(properties.containsKey(PORT)){
            mysql.put(PORT, properties.get(PORT));
        }

        this.updateInstanceGroupConfiguration(manifest, plan);
    }
}
