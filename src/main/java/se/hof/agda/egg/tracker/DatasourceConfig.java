package se.hof.agda.egg.tracker;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class DatasourceConfig implements ConfigSource {

    Map<String, String> properties = new HashMap<>();

    public DatasourceConfig() {
        String databaseUrl = System.getenv("DATABASE_URL");
        System.out.println("Profile: " + System.getProperties().getProperty(
                "quarkus.profile","not set"));
        if (databaseUrl != null) {
            parseHerokuDatabaseUrl(databaseUrl);
        } else
            setDefaultProperties();
    }

    private void setDefaultProperties() {
        String profile = System.getProperties().getProperty(
                "quarkus.profile","prod");

        switch (profile) {
            case "dev":
                System.out.println("Using dev datasource config");
//                properties.put("quarkus.datasource.url", "jdbc:postgresql:postgres");
//                properties.put("quarkus.datasource.username", "dev");
//                properties.put("quarkus.datasource.password", "agda");
                break;
            //case "test":
            default:
                properties.put("quarkus.datasource.driver", "org.h2.Driver");
                properties.put("quarkus.datasource.url", "jdbc:h2:tcp://localhost/mem:dev");
                properties.put("quarkus.datasource.username", "username-dev");
                properties.put("quarkus.datasource.password", "agda");
        }
    }

    private void parseHerokuDatabaseUrl(String databaseUrl) {
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
        return 99;
    }
}
