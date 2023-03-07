# SupersetApiClient-java

- This is a Java-based Superset API client that provides dashboard export and import functions.
- Support Superset version: 2.0.0

# Build

```shell
mvn install
```

# Usage

First run the Maven build command to install the artifact into your local Maven repository then add the dependency to your maven project.

```xml
<dependency>
    <groupId>scott</groupId>
    <artifactId>superset-api</artifactId>
    <version>1.0</version>
</dependency>
```

```java
superset.client.Client client = new Client(ip, port, username, password);

// list all dashboard
client.dashboards();

// export specific dashbaord to a zip file
client.exportDashboard(dashboardId, saveFileDestination);

// import dashboard
client.importDashboard(dashboardFile, password, isOverride)
```
