/**
 * 
 */
package de.evoila.cf.broker.custom.mysql;

import de.evoila.cf.broker.model.ServerAddress;
import de.evoila.cf.broker.util.ServiceInstanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Johannes Hiemer
 *
 */
public class MySQLDbService {

	private Logger log = LoggerFactory.getLogger(getClass());

	private Connection connection;

	public boolean createConnection(String username, String password, String database, List<ServerAddress> serverAddresses) {
        String connectionUrl = ServiceInstanceUtils.connectionUrl(serverAddresses);

        try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String url = "jdbc:mysql://" + connectionUrl + "/" + database;
			connection = DriverManager.getConnection(url, username, password);
		} catch (ClassNotFoundException | SQLException | InstantiationException | IllegalAccessException e) {
			log.info("Could not establish connection", e);
			return false;
		}
		return true;
	}

	public void executeUpdate(String query) throws SQLException {
		Statement statement = connection.createStatement();

		try {
			log.debug("Executing the following query: " + query);
			statement.execute(query);
		} catch (SQLException e) {
			log.error(e.toString());
		} finally {
			statement.close();
		}
	}

	public Map<String, String> executeSelect(String query) throws SQLException {
		Statement statement = connection.createStatement();

		try {
            Map<String, String> resultMap = new HashMap<>();
			ResultSet result = statement.executeQuery(query);

            String column = "database";
            while(result.next()) {
                resultMap.put(result.getString(column), result.getString(column));
            }

			return resultMap;
		} catch (SQLException e) {
			log.error(e.toString());
			return null;
		}
	}

	public void executePreparedUpdate(String query, Map<Integer, String> parameterMap) throws SQLException {
		if (parameterMap == null) {
			throw new SQLException("parameterMap cannot be empty");
		}

		PreparedStatement preparedStatement = connection.prepareStatement(query);

		for (Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
			preparedStatement.setString(parameter.getKey(), parameter.getValue());
		}

		try {
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			log.error(e.toString());
		} finally {
			preparedStatement.close();
		}
	}

	public Map<String, String> executePreparedSelect(String query, Map<Integer, String> parameterMap)
			throws SQLException {
		if (parameterMap == null) {
			throw new SQLException("parameterMap cannot be empty");
		}

		PreparedStatement preparedStatement = connection.prepareStatement(query);

		for (Map.Entry<Integer, String> parameter : parameterMap.entrySet()) {
			preparedStatement.setString(parameter.getKey(), parameter.getValue());
		}

		try {
			ResultSet result = preparedStatement.executeQuery();
			ResultSetMetaData resultMetaData = result.getMetaData();
			int columns = resultMetaData.getColumnCount();

			Map<String, String> resultMap = new HashMap<String, String>(columns);

			if (result.next()) {
				for (int i = 1; i <= columns; i++) {
					resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
				}
			}

			return resultMap;
		} catch (SQLException e) {
			log.error(e.toString());
			return null;
		}
	}
}
