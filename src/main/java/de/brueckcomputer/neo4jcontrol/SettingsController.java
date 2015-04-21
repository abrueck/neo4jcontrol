package de.brueckcomputer.neo4jcontrol;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * @author Andreas Brueck
 */
public class SettingsController implements Initializable {

    private Context context;

    @FXML
    private TextField serverPath;

    @FXML
    private TextField statusUrl;

    @FXML
    private TextField username;

    @FXML
    private PasswordField password;

    @FXML
    private TextField browserUrl;

    @FXML
    private TextField documentationUrl;

    @FXML
    private TextField refcardUrl;


    public SettingsController() {
        context = Context.getInstance();
    }


    private void closeWindow() {
        Stage stage = (Stage) serverPath.getScene().getWindow();
        stage.close();
    }


    @FXML
    public void handleOk(ActionEvent event) {
        Properties settings = context.getSettings();
        settings.setProperty(Context.SERVER_PATH, serverPath.getText());
        settings.setProperty(Context.STATUS_URL, statusUrl.getText());
        settings.setProperty(Context.SERVER_USER, username.getText());
        settings.setProperty(Context.SERVER_PASSWORD, password.getText());
        settings.setProperty(Context.BROWSER_URL, browserUrl.getText());
        settings.setProperty(Context.DOCUMENTATION_URL, documentationUrl.getText());
        settings.setProperty(Context.REFCARD_URL, refcardUrl.getText());
        context.storeSettings();

        closeWindow();
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        closeWindow();
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Properties settings = context.getSettings();
        serverPath.setText(settings.getProperty(Context.SERVER_PATH));
        statusUrl.setText(settings.getProperty(Context.STATUS_URL));
        username.setText(settings.getProperty(Context.SERVER_USER));
        password.setText(settings.getProperty(Context.SERVER_PASSWORD));
        browserUrl.setText(settings.getProperty(Context.BROWSER_URL));
        documentationUrl.setText(settings.getProperty(Context.DOCUMENTATION_URL));
        refcardUrl.setText(settings.getProperty(Context.REFCARD_URL));
    }
}
