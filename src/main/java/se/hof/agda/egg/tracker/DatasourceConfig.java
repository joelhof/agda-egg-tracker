package se.hof.agda.egg.tracker;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class DatasourceConfig implements ConfigSource {

    Map<String, String> properties = new HashMap<>();

    public DatasourceConfig() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null) {
            parseDatabaseUrl(databaseUrl);
        } else
            setDefaultProperties();
    }

    private void setDefaultProperties() {
        properties.put("quarkus.datasource.url", "");
        properties.put("quarkus.datasource.username", "");
        properties.put("quarkus.datasource.password", "");
    }

    private void parseDatabaseUrl(String databaseUrl) {
        try {
            URI dbUri = new URI(databaseUrl);
            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String dbUrl = "jdbc:postgresql://"
                    + dbUri.getHost() + ':'
                    + dbUri.getPort()
                    + dbUri.getPath()
                    + "?sslmode=require";

            properties.put("quarkus.datasource.url", dbUrl);
            properties.put("quarkus.datasource.username", username);
            properties.put("quarkus.datasource.password", password);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String getValue(String s) {
        return properties.get(s);
    }

    @Override
    public String getName() {
        return DatasourceConfig.class.getName();
    }

    @Override
    public Set<String> getPropertyNames() {
        return new HashSet<>(
                Arrays.asList("quarkus.datasource.url",
                              "quarkus.datasource.username",
                              "quarkus.datasource.password"));
    }

    @Override
    public int getOrdinal() {
        return 101;
    }
}
