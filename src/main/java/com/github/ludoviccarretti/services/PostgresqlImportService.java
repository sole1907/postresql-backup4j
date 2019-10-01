package com.github.ludoviccarretti.services;

import com.github.ludoviccarretti.model.InformationSchemaGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by lcarretti on 30-Sep-19.
 */
public class PostgresqlImportService {

    private String database;
    private String username;
    private String password;
    private String sqlString;
    private String jdbcConnString;
    private String jdbcDriver;
    private boolean deleteExisting;
    private boolean dropExisting;
    private List<InformationSchemaGenerator> tables;
    private Logger logger = LoggerFactory.getLogger(PostgresqlImportService.class);

    private PostgresqlImportService() {
        this.deleteExisting = false;
        this.dropExisting = false;
        this.tables = new ArrayList<>();
    }

    /**
     * @return bool
     * @throws SQLException           exception
     * @throws ClassNotFoundException exception
     */
    public boolean importDatabase() throws SQLException, ClassNotFoundException {

        if (!this.assertValidParams()) {
            logger.error("Required Parameters not set or empty \n" +
                    "Ensure database, username, password, sqlString params are configured \n" +
                    "using their respective setters");
            return false;
        }


        //connect to the database
        Connection connection;
        if (jdbcConnString == null || jdbcConnString.isEmpty()) {
            connection = PostgresqlBaseService.connect(username, password,
                    database, jdbcDriver);
        } else {

            if (jdbcConnString.contains("?")) {
                database = jdbcConnString.substring(jdbcConnString.lastIndexOf("/") + 1, jdbcConnString.indexOf("?"));
            } else {
                database = jdbcConnString.substring(jdbcConnString.lastIndexOf("/") + 1);
            }

            logger.debug("database name extracted from connection string: " + database);
            connection = PostgresqlBaseService.connectWithURL(username, password,
                    jdbcConnString, jdbcDriver);
        }

        Statement stmt = connection.createStatement();

        if (deleteExisting || dropExisting) {

            //get all the tables, so as to eliminate delete errors due to non-existent tables
            tables = PostgresqlBaseService.getAllTables(stmt);
            logger.debug("tables found for deleting/dropping: \n" + tables.toString());

            //execute delete query
            for (InformationSchemaGenerator table : tables) {

                //if deleteExisting and dropExisting is true
                //skip the deleteExisting query
                //dropExisting will take care of both
                if (deleteExisting && !dropExisting) {
                    String delQ = "DELETE FROM '" + table.getName() + "';";
                    logger.debug("adding " + delQ + " to batch");
                    stmt.addBatch(delQ);
                }

                if (dropExisting) {
                    String dropQ = "DROP TABLE IF EXISTS '" + table.getName() + "'";
                    logger.debug("adding " + dropQ + " to batch");
                    stmt.addBatch(dropQ);
                }

            }
        }

        //disable foreign key check
        stmt.addBatch("SET session_replication_role = 'replica';");


        //now process the sql string supplied
        while (sqlString.contains(PostgresqlBaseService.SQL_START_PATTERN)) {

            //get the chunk of the first statement to execute
            int startIndex = sqlString.indexOf(PostgresqlBaseService.SQL_START_PATTERN);
            int endIndex = sqlString.indexOf(PostgresqlBaseService.SQL_END_PATTERN);

            String executable = sqlString.substring(startIndex, endIndex);
            logger.debug("adding extracted executable SQL chunk to batch : \n" + executable);
            stmt.addBatch(executable);

            //remove the chunk from the whole to reduce it
            sqlString = sqlString.substring(endIndex + 1);

            //repeat
        }


        //add enable foreign key check
        stmt.addBatch("SET session_replication_role = 'origin';");

        //now execute the batch
        long[] result = stmt.executeLargeBatch();

        String resultString = Arrays.stream(result)
                .mapToObj(String::valueOf)
                .reduce("", (s1, s2) -> s1 + ", " + s2 + ", ");
        logger.debug(result.length + " queries were executed in batches for provided SQL String with the following result : \n" + resultString);

        stmt.close();
        connection.close();

        return true;
    }

    /**
     * This function will check that required parameters
     * are set
     *
     * @return bool
     */
    private boolean assertValidParams() {
        return username != null && !this.username.isEmpty() &&
                password != null && !this.password.isEmpty() &&
                sqlString != null && !this.sqlString.isEmpty() &&
                ((database != null && !this.database.isEmpty()) || (jdbcConnString != null && !jdbcConnString.isEmpty()));
    }

    /**
     * This function will create a new
     * PostgresqlImportService instance thereby facilitating
     * a builder pattern
     *
     * @return PostgresqlImportService
     */
    public static PostgresqlImportService builder() {
        return new PostgresqlImportService();
    }

    public PostgresqlImportService setDatabase(String database) {
        this.database = database;
        return this;
    }

    public PostgresqlImportService setUsername(String username) {
        this.username = username;
        return this;
    }

    public PostgresqlImportService setPassword(String password) {
        this.password = password;
        return this;
    }

    public PostgresqlImportService setSqlString(String sqlString) {
        this.sqlString = sqlString;
        return this;
    }

    public PostgresqlImportService setDeleteExisting(boolean deleteExisting) {
        this.deleteExisting = deleteExisting;
        return this;
    }

    public PostgresqlImportService setDropExisting(boolean dropExistingTable) {
        this.dropExisting = dropExistingTable;
        return this;
    }

    public PostgresqlImportService setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
        return this;
    }

    public PostgresqlImportService setJdbcConnString(String jdbcConnString) {
        this.jdbcConnString = jdbcConnString;
        return this;
    }
}
