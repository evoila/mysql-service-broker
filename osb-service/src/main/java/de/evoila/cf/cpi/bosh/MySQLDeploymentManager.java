package de.evoila.cf.cpi.bosh;

import de.evoila.cf.broker.bean.BoshProperties;
import de.evoila.cf.broker.custom.mysql.MySQLUtils;
import de.evoila.cf.broker.model.Plan;
import de.evoila.cf.broker.model.ServiceInstance;
import de.evoila.cf.broker.util.RandomString;
import de.evoila.cf.cpi.bosh.deployment.DeploymentManager;
import de.evoila.cf.cpi.bosh.deployment.manifest.Manifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by reneschollmeyer, evoila on 28.02.18.
 */
public class MySQLDeploymentManager extends DeploymentManager {

    private RandomString randomStringUsername = new RandomString(10);
    private RandomString randomStringPassword = new RandomString(15);

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
    protected void replaceParameters(ServiceInstance serviceInstance, Manifest manifest, Plan plan, Map<String, Object> customParameters) {
        HashMap<String, Object> properties = new HashMap<>();
        if (customParameters != null && !customParameters.isEmpty())
            properties.putAll(customParameters);

        log.debug("Updating Deployment Manifest, replacing parameters");

        Map<String, Object> manifestProperties = manifest.getInstanceGroups()
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

        String password = randomStringPassword.nextString();
        String username = randomStringUsername.nextString();

        List<Map<String, String>> databases = new ArrayList<>();
        Map<String, String> database = new HashMap<>();
        database.put("name", MySQLUtils.dbName(serviceInstance.getId()));
        database.put("username", username);
        database.put("password", password);
        databases.add(database);
        mysql.put("seeded_databases", databases);

        serviceInstance.setUsername(username);
        serviceInstance.setPassword(password);

        mysqldExporter.put(MYSQLD_EXPORTER_PASSWORD, password);
        mysql.put(MYSQL_ADMIN_PASSWORD, password);
        clusterHealth.put(MYSQL_CLUSTER_HEALTH_PASSWORD, password);
        galeraHealthcheck.put(GALERA_ENDPOINT_PASSWORD, password);
        galeraHealthcheck.put(GALERA_DB_PASSWORD, password);

        if(properties.containsKey(DATA_PATH)) {
            mysql.put(DATA_PATH, properties.get(DATA_PATH));
        }

        if(properties.containsKey(PORT)){
            mysql.put(PORT, properties.get(PORT));
        }

        this.updateInstanceGroupConfiguration(manifest, plan);
    }
}
