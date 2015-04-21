package de.brueckcomputer.neo4jcontrol;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.ws.rs.core.MediaType;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Andreas Brueck
 *
 * Uses idea and sourcecode from https://gist.github.com/jewelsea/e231e89e8d36ef4e5d8a
 */
public class MainApp extends Application {

    private MenuItem launcherItem;
    private String serverResponse;
    private int serverStatus;
    private Context context;

    public MainApp() {
        context = Context.getInstance();
    }

    // sets up the javafx application.
    // a tray icon is setup for the icon, but the main stage remains invisible until the user
    // interacts with the tray icon.
    @Override
    public void start(final Stage stage) {
        // stores a reference to the stage.
        context.setMainStage(stage);

        // instructs the javafx system not to exit implicitly when the last application window is shut.
        Platform.setImplicitExit(false);

        // sets up the tray icon (using awt code run on the swing thread).
        javax.swing.SwingUtilities.invokeLater(this::addAppToTray);

    }


    /**
     * Sets up a system tray icon for the application.
     */
    private void addAppToTray() {
        try {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // app requires system tray support, just exit if there is no support.
            if (!java.awt.SystemTray.isSupported()) {
                System.out.println("No system tray support, application exiting.");
                Dialogs.showError("No system tray support, application exiting.");
                Platform.exit();
            }

            // set up a system tray icon.
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            java.awt.Image image = ImageIO.read(getClass().getResource("neo4j.png"));
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image);

            // if the user double-clicks on the tray icon, show the main app stage.
            trayIcon.addActionListener(event -> Platform.runLater(this::openBrowser));


            // setup the popup menu for the application.
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();

            // if the user selects the default menu item (which includes the app name),
            // show the main app stage.
            launcherItem = new MenuItem("Launch Neo4j");
            launcherItem.addActionListener(event -> Platform.runLater(this::launcherClick));
            popup.add(launcherItem);
            refreshServerState(true);

            MenuItem item = new MenuItem("Status");
            item.addActionListener(event -> Platform.runLater(this::showStatus));
            popup.add(item);

            popup.addSeparator();

            item = new MenuItem("Neo4j Browser");
            item.addActionListener(event -> Platform.runLater(this::openBrowser));
            // the convention for tray icons seems to be to set the default icon for opening
            // the application stage in a bold font.
            java.awt.Font defaultFont = java.awt.Font.decode(null);
            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
            item.setFont(boldFont);
            popup.add(item);

            item = new MenuItem("Documentation");
            item.addActionListener(event -> Platform.runLater(this::openDocumentation));
            popup.add(item);

            item = new MenuItem("Cypher Refcard");
            item.addActionListener(event -> Platform.runLater(this::openRefcard));
            popup.add(item);

            popup.addSeparator();

            item = new MenuItem("Settings");
            item.addActionListener(event -> Platform.runLater(this::showSettings));
            popup.add(item);

            // to really exit the application, the user must go to the system tray icon
            // and select the exit option, this will shutdown JavaFX and remove the
            // tray icon (removing the tray icon will also shut down AWT).

            item = new java.awt.MenuItem("Exit");
            item.addActionListener(event -> {
                Platform.exit();
                tray.remove(trayIcon);
            });
            popup.add(item);
            trayIcon.setPopupMenu(popup);

            tray.add(trayIcon);

        } catch (java.awt.AWTException | IOException e) {
            System.out.println("Unable to init system tray");
            e.printStackTrace();
            Dialogs.showExceptionDialog(e);
        }
    }

    private void showStatus() {
        refreshServerState(true);
        Dialogs.showInformation(
                String.format("GET on [%s], status code [%d]", context.getSettings().getProperty(Context.STATUS_URL), serverStatus),
                serverResponse);
    }


    private void showSettings() {
        if (context.getMainStage() != null) {
            try {
                Stage stage = new Stage();
                Parent root = FXMLLoader.load(getClass().getResource("SettingsDialog.fxml"));
                Scene scene = new Scene(root);
                stage.setTitle("Configuration");
                stage.setScene(scene);
                stage.initOwner(context.getMainStage());
                stage.initModality(Modality.WINDOW_MODAL);
                stage.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                Dialogs.showExceptionDialog(e);
            }
            refreshServerState(true);
        }
    }

    private void refreshServerState(boolean printResponse) {
        serverResponse = null;
        serverStatus = -1;

        try {
            Client client = Client.create();
            String user = context.getSettings().getProperty(Context.SERVER_USER);
            if (user != null && !user.isEmpty() ) {
                String password = context.getSettings().getProperty(Context.SERVER_PASSWORD);
                client.addFilter(new HTTPBasicAuthFilter(user, password));
            }
            WebResource resource = client.resource(context.getSettings().getProperty(Context.STATUS_URL));
            ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON).get(ClientResponse.class);

            serverStatus = response.getStatus();
            if (response.getStatus() == 200 || response.getStatus() == 401) {
                serverResponse = response.getEntity(String.class);
                launcherItem.setLabel("Stop Neo4j");
            }
            if (printResponse) {
                System.out.println(String.format("GET on [%s], status code [%d]", context.getSettings().getProperty(Context.STATUS_URL), serverStatus));
                if (serverResponse != null) {
                    System.out.println(serverResponse);
                }
            }
            response.close();

        } catch (Exception e) {
            launcherItem.setLabel("Launch Neo4j");

        }


        launcherItem.setEnabled(context.validServerPath());
    }


    private String runCommand(String command) {
        final Process p;
        try {
            p = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
            Dialogs.showExceptionDialog(e);
            return null;
        }

        StringBuffer commandOutput = new StringBuffer();
        new Thread(() -> {
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;

            try {
                while ((line = input.readLine()) != null) {
                    System.out.println(line);
                    commandOutput.append(line).append('\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Dialogs.showExceptionDialog(e);
        }
        return commandOutput.toString();
    }


    private void launcherClick() {
        refreshServerState(false);
        String command;
        if (serverResponse == null) {
            command = context.getServerCommand().toAbsolutePath().toString() + " start";
        } else {
            command = context.getServerCommand().toAbsolutePath().toString() + " stop";
        }
        String output = runCommand(command);
        refreshServerState(true);
        String message;
        if (serverResponse == null) {
            message = "Server is not running.";
        } else {
            message = "Server is running.";
        }
        Dialogs.showInformation(message, output);
    }

    private void showWebsite(String website) {
        HostServicesDelegate hostServices = HostServicesFactory.getInstance(this);
        hostServices.showDocument(website);
    }


    private void openBrowser() {
        showWebsite(context.getSettings().getProperty(Context.BROWSER_URL));
    }

    private void openDocumentation() {
        showWebsite(context.getSettings().getProperty(Context.DOCUMENTATION_URL));
    }

    private void openRefcard() {
        showWebsite(context.getSettings().getProperty(Context.REFCARD_URL));
    }

    public static void main(String[] args) throws IOException, java.awt.AWTException {
        launch(args);
    }


}
