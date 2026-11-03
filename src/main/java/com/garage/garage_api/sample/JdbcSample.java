package com.garage.garage_api.sample;

import java.sql.*;

public class JdbcSample {

    // conncect to db using jdbc apis
    // for this we might need the posthressdriver so that we can connec to db because that implements the
    // jdbc apis


    public static void main(String[] args) {

        // load the properties file
        // some data source var datasource = new HikariDataSource();
        // datasource.
        // replace this with datasource
        // this is ur jdbc api and this the driver implementations / jdbc apis have these interfaces
        String url = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5433/garage");
        String user = System.getenv().getOrDefault("DB_USERNAME", "garage");
        String password = System.getenv().getOrDefault("DB_PASSWORD","garage-local");
        if (password == null) {
            throw new IllegalStateException("DB_PASSWORD env var is not set");
        }
        String sql = "SELECT 1 FROM enquiry LIMIT 1";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            System.out.println(resultSet.next());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
