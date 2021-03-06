postgresql-backup4j
==============

postgresql-backup4j is a library for programmatically exporting postgresql databases 
and sending the zipped dump to email, Amazon S3, Google Drive or any other cloud storage of choice

**It gives the developer access to the generated zip file and the generated SQL query string**
 for use in other part of the application. 

**It also provides a method for importing the SQL exported by the tool - programmatically.**

Installation
============
The artifact is available on Maven Central and can be added to the project's pom.xml:

```xml
<dependency>
    <groupId>com.github.ludoviccarretti</groupId>
    <artifactId>postgresql-backup4j</artifactId>
    <version>1.0.1</version>
</dependency>
```

The latest version can be found [here](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.ludoviccarretti%22%20a%3A%22postgresql-backup4j%22)

Usage
=====
The minimum configuration required for the library is the database name, username and password.

However, if you want the backup file to be sent to your email automatically after backup, you must 
provide email configurations as well.

```java
//required properties for exporting of db
Properties properties = new Properties();
properties.setProperty(PropertiesOptions.DB_NAME, "database-name");
properties.setProperty(PropertiesOptions.DB_USERNAME, "root");
properties.setProperty(PropertiesOptions.DB_PASSWORD, "root");
        
//properties relating to email config
properties.setProperty(PropertiesOptions.EMAIL_HOST, "smtp.mailtrap.io");
properties.setProperty(PropertiesOptions.EMAIL_PORT, "25");
properties.setProperty(PropertiesOptions.EMAIL_USERNAME, "mailtrap-username");
properties.setProperty(PropertiesOptions.EMAIL_PASSWORD, "mailtrap-password");
properties.setProperty(PropertiesOptions.EMAIL_FROM, "test@smattme.com");
properties.setProperty(PropertiesOptions.EMAIL_TO, "backup@smattme.com");

//set the outputs temp dir
properties.setProperty(PropertiesOptions.TEMP_DIR, new File("external").getPath());

PostgresqlExportService postgresqlExportService = new PostgresqlExportService(properties);
postgresqlExportService.export();
```

Calling `postgresqlExportService.export();` will export the database and save the dump temporarily in the configured `TEMP_DIR`

If an email config is supplied, the dump will be sent as an attachment. Finally, when all operations are completed the 
temporary dir is cleared and deleted.

If you want to get the generated backup file as a Java `File` object, you need to specify this property as part of the 
configuration:

```java
//...
properties.setProperty(PostgresqlExportService.PRESERVE_GENERATED_ZIP, "true");
```

and then you can call this method:

```java
File file = postgresqlExportService.getGeneratedZipFile();
```

**Because you set preserve generated file to be true, the library will not clear the temp dir as expected 
and you have to do that manually by calling this method:**

```java
postgresqlExportService.clearTempFiles(false);
```

Finally, let's say for some reason you want the generated SQL string you can do this:

```java
String generatedSql = postgresqlExportService.getGeneratedSql();
```

Other parameters are:

```java
properties.setProperty(PropertiesOptions.ADD_IF_NOT_EXISTS, "true");
properties.setProperty(PropertiesOptions.JDBC_DRIVER_NAME, "root.ss");
properties.setProperty(PropertiesOptions.JDBC_CONNECTION_STRING, "jdbc:postgresql://localhost:5432/database-name");
```

They are explained in a detailed manner in this [tutorial](https://smattme.com/blog/technology/how-to-backup-mysql-database-programmatically-using-mysql-backup4j)

Importing a Database
--------------------
To import a database, you need to use the ImportService like so:

```java
String sql = new String(Files.readAllBytes(Paths.get("path/to/sql/dump/file.sql")));

boolean res = PostgresqlImportService.builder()
        .setDatabase("database-name")
        .setSqlString(sql)
        .setUsername("root")
        .setPassword("root")
        .setDeleteExisting(true)
        .setDropExisting(true)
        .importDatabase();
        
assertTrue(res);
```

First get SQL as a String and then pass it to the import service with the right configurations.

Alternatively, you can also use the `.setJdbcConnString(jdbcURL)` method on the import service.

e.g. 
```java
boolean res = PostgresqlImportService.builder()
                .setSqlString(generatedSql)
                .setJdbcConnString("jdbc:postgresql://localhost:5432/backup4j_test")
                .setUsername("db-username")
                .setPassword("db-password")
                .setDeleteExisting(true)
                .setDropExisting(true)
                .importDatabase();
```

`setDeleteExisting(true)` will **delete all data** from existing tables in the target database. 

While `setDropExisting(true)` will **drop** the table. 

Supplying `false` to these functions will disable their respective actions.


**NOTE: The import service is only guaranteed to work with SQL files generated by the export service of this library**

Contributions and Support
=========================
**Love this project or found it useful? You can [buy me a cup of coffee](http://wallet.ng/pay/ossmatt)** :coffee:

If you want to create a new feature, though not compulsory, but it will be helpful to reach out to me first before proceeding.

To avoid a scenario where you submit a PR for an issue that someone else is working on already.


Tutorials / Articles
====================
- For mysql: [https://smattme.com/blog/technology/how-to-backup-mysql-database-programmatically-using-mysql-backup4j](https://smattme.com/blog/technology/how-to-backup-mysql-database-programmatically-using-mysql-backup4j)

- Add your own here.