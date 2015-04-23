package de.brueckcomputer.neo4jcontrol;

import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;

/**
 * @author Andreas Brueck
 */
public class Context {

    public final static String SERVER_PATH = "server.path";
    public final static String SERVER_START = "server.start";
    public final static String SERVER_STOP = "server.stop";
    public final static String SERVER_USER = "server.user";
    public final static String SERVER_PASSWORD = "server.password";
    public final static String STATUS_URL = "status.url";
    public final static String BROWSER_URL = "browser.url";
    public final static String DOCUMENTATION_URL = "documentation.url";
    public final static String REFCARD_URL = "refcard.url";

    private final static Context instance = new Context();

    public static Context getInstance() {
        return instance;
    }

    private Stage mainStage;
    private Properties settings;
    private Path settingsFile;


    private Context() {

        // Configuration
        Properties defaultConfiguration = new Properties();
        defaultConfiguration.setProperty(STATUS_URL, "http://localhost:7474/db/data/");
        defaultConfiguration.setProperty(BROWSER_URL, "http://localhost:7474");
        defaultConfiguration.setProperty(DOCUMENTATION_URL, "http://neo4j.com/docs/stable/");
        defaultConfiguration.setProperty(REFCARD_URL, "http://neo4j.com/docs/stable/cypher-refcard/");
        settings = new java.util.Properties(defaultConfiguration);
        settingsFile = Paths.get(System.getProperty("user.home"), "neo4jcontrol.properties");
        loadSettings();

    }

    private void loadSettings() {
        if (Files.exists(settingsFile)) {
            try {
                settings.load(Files.newBufferedReader(settingsFile));
            } catch (IOException e) {
                e.printStackTrace();
                Dialogs.showExceptionDialog(e);
                Platform.exit();
            }
        } else {
            System.err.println("Missing settings file " + settingsFile.toString());
        }
    }

    public void storeSettings() {
        String comment = (new Date()).toString();
        try {
            settings.store(Files.newBufferedWriter(settingsFile), comment);
        } catch (IOException e) {
            e.printStackTrace();
            Dialogs.showExceptionDialog(e);
            Platform.exit();
        }
    }


    public boolean validServerPath() {
        return getServerCommand(true) != null && getServerCommand(false) != null;
    }


    public String getServerCommand(boolean launch) {
        String serverPath = settings.getProperty(SERVER_PATH);
        String startCmd = settings.getProperty(SERVER_START);
        String stopCmd = settings.getProperty(SERVER_STOP);

        if (launch && startCmd != null && !startCmd.isEmpty()) {
            return startCmd;
        }
        if (!launch && stopCmd != null && !stopCmd.isEmpty()) {
            return stopCmd;
        }

        if (serverPath == null || serverPath.isEmpty()) {
            return null;
        }
        if (Files.exists(Paths.get(serverPath,"bin","neo4j"))) {
            return Paths.get(serverPath,"bin","neo4j").toAbsolutePath().toString() + (launch ? " start" : " stop");
        }
        if (Files.exists(Paths.get(serverPath,"bin","neo4j.bat"))) {
            return Paths.get(serverPath,"bin","neo4j.bat").toAbsolutePath().toString() + (launch ? " start" : " stop");
        }
        return null;
    }

//    public Path getShettCommand() {
//        String serverPath = settings.getProperty(SERVER_PATH);
//        if (serverPath == null || serverPath.isEmpty()) {
//            return null;
//        }
//        if (Files.exists(Paths.get(serverPath,"bin","neo4j-shell") {
//            return Paths.get(serverPath,"bin","neo4j-shell");
//        }
//        if (Files.exists(Paths.get(serverPath,"bin","neo4jshell.bat") {
//            return Paths.get(serverPath,"bin","neo4jshell.bat");
//        }
//        return null;
//    }



    public Stage getMainStage() {
        return mainStage;
    }

    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }

    public Properties getSettings() {
        return settings;
    }

}
