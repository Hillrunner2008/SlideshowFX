package com.twasyl.slideshowfx.hosting.connector.box;

import com.box.sdk.BoxAPIConnection;
import com.twasyl.slideshowfx.engine.presentation.PresentationEngine;
import com.twasyl.slideshowfx.global.configuration.GlobalConfiguration;
import com.twasyl.slideshowfx.hosting.connector.AbstractHostingConnector;
import com.twasyl.slideshowfx.hosting.connector.exceptions.HostingConnectorException;
import com.twasyl.slideshowfx.hosting.connector.io.RemoteFile;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This connector allows to interact with Box.
 *
 * @author Thierry Wasylczenko
 * @version 1.0
 * @since SlideshowFX @@NEXT-VERSION@@
 */
public class BoxHostingConnector extends AbstractHostingConnector<BoxHostingConnectorOptions> {
    private static final Logger LOGGER = Logger.getLogger(BoxHostingConnector.class.getName());

    protected static final String REFRESH_TOKEN_PROPERTY_SUFFIX = ".refreshtoken";

    private String refreshToken;
    private BoxAPIConnection boxApi;

    public BoxHostingConnector() {
        super("box", "Box", new RemoteFile(null));

        this.setOptions(new BoxHostingConnectorOptions());

        String configuration = GlobalConfiguration.getProperty(getConfigurationBaseName().concat(CONSUMER_KEY_PROPERTY_SUFFIX));
        if(configuration != null && !configuration.trim().isEmpty()) {
            this.getOptions().setRefreshToken(configuration.trim());
        }

        configuration = GlobalConfiguration.getProperty(getConfigurationBaseName().concat(CONSUMER_SECRET_PROPERTY_SUFFIX));
        if(configuration != null && !configuration.trim().isEmpty()) {
            this.getOptions().setConsumerSecret(configuration.trim());
        }

        configuration = GlobalConfiguration.getProperty(getConfigurationBaseName().concat(REDIRECT_URI_PROPERTY_SUFFIX));
        if(configuration != null && !configuration.trim().isEmpty()) {
            this.getOptions().setRedirectUri(configuration.trim());
        }

        configuration = GlobalConfiguration.getProperty(getConfigurationBaseName().concat(ACCESS_TOKEN_PROPERTY_SUFFIX));
        if(configuration != null && !configuration.trim().isEmpty()) {
            this.accessToken = configuration;
        }

        configuration = GlobalConfiguration.getProperty(getConfigurationBaseName().concat(REFRESH_TOKEN_PROPERTY_SUFFIX));
        if(configuration != null && !configuration.trim().isEmpty()) {
            this.refreshToken = configuration;
        }

        if(this.getOptions().getConsumerKey() != null && this.getOptions().getConsumerSecret() != null) {
            this.boxApi = new BoxAPIConnection(this.getOptions().getConsumerKey(), this.getOptions().getConsumerSecret());

            if(this.accessToken != null && !this.accessToken.isEmpty()) {
                this.boxApi.setAccessToken(this.accessToken);
            }

            if(this.refreshToken != null && !this.refreshToken.isEmpty()) {
                this.boxApi.setRefreshToken(this.refreshToken);
            }
        }
    }

    @Override
    public Node getConfigurationUI() {
        this.newOptions = new BoxHostingConnectorOptions();
        this.newOptions.setConsumerKey(this.getOptions().getConsumerKey());
        this.newOptions.setConsumerSecret(this.getOptions().getConsumerSecret());
        this.newOptions.setRedirectUri(this.getOptions().getRedirectUri());

        final Label consumerKeyLabel = new Label("Consumer key:");
        final Label consumerSecretLabel = new Label("Consumer secret:");
        final Label redirectUriLabel = new Label("Redirect URI:");

        final TextField consumerKeyTextField = new TextField();
        consumerKeyTextField.textProperty().bindBidirectional(this.newOptions.consumerKeyProperty());
        consumerKeyTextField.setPrefColumnCount(20);

        final TextField consumerSecretTextField = new TextField();
        consumerSecretTextField.textProperty().bindBidirectional(this.newOptions.consumerSecretProperty());
        consumerSecretTextField.setPrefColumnCount(20);

        final TextField redirectUriTextField = new TextField();
        redirectUriTextField.textProperty().bindBidirectional(this.newOptions.redirectUriProperty());
        redirectUriTextField.setPrefColumnCount(20);

        final HBox consumerKeyBox = new HBox(5, consumerKeyLabel, consumerKeyTextField);
        consumerKeyBox.setAlignment(Pos.BASELINE_LEFT);

        final HBox consumerSecretBox = new HBox(5, consumerSecretLabel, consumerSecretTextField);
        consumerSecretBox.setAlignment(Pos.BASELINE_LEFT);

        final HBox redirectUriBox = new HBox(5, redirectUriLabel, redirectUriTextField);
        redirectUriBox.setAlignment(Pos.BASELINE_LEFT);

        final VBox container = new VBox(5, consumerKeyBox, consumerSecretBox, redirectUriBox);

        return container;
    }

    @Override
    public void saveNewOptions() {
        if(this.getNewOptions() != null) {
            this.setOptions(this.getNewOptions());

            if (this.getOptions().getConsumerKey() != null) {
                GlobalConfiguration.setProperty(getConfigurationBaseName().concat(CONSUMER_KEY_PROPERTY_SUFFIX),
                        this.getOptions().getConsumerKey());
            }

            if (this.getOptions().getConsumerSecret() != null) {
                GlobalConfiguration.setProperty(getConfigurationBaseName().concat(CONSUMER_SECRET_PROPERTY_SUFFIX),
                        this.getOptions().getConsumerSecret());
            }

            if (this.getOptions().getRedirectUri() != null) {
                GlobalConfiguration.setProperty(getConfigurationBaseName().concat(REDIRECT_URI_PROPERTY_SUFFIX),
                        this.getOptions().getRedirectUri());
            }

            if(this.getOptions().getConsumerKey() != null && this.getOptions().getConsumerSecret() != null) {
                this.boxApi = new BoxAPIConnection(this.getOptions().getConsumerKey(), this.getOptions().getConsumerSecret());
            }
        }
    }

    @Override
    public void authenticate() throws HostingConnectorException {
        if(this.boxApi == null) throw new HostingConnectorException(HostingConnectorException.MISSING_CONFIGURATION);

        final WebView browser = new WebView();
        final Scene scene = new Scene(browser);
        final Stage stage = new Stage();

        browser.setPrefSize(500, 500);

        browser.getEngine().locationProperty().addListener((locationProperty, oldLocation, newLocation) -> {
            if(newLocation != null && newLocation.startsWith(this.getOptions().getRedirectUri())) {
                System.out.println(newLocation);
                try {
                    final Map<String, String> uriParameters = getURIParameters(new URI(newLocation));

                    if(uriParameters.containsKey("code")) {
                        this.boxApi.authenticate(uriParameters.get("code"));
                        this.accessToken = this.boxApi.getAccessToken();
                    }
                } catch (URISyntaxException e) {
                    LOGGER.log(Level.SEVERE, "Error when parsing the redirect URI", e);
                } finally {
                    if (this.accessToken != null) {
                        GlobalConfiguration.setProperty(getConfigurationBaseName().concat(ACCESS_TOKEN_PROPERTY_SUFFIX), this.accessToken);
                    }
                    stage.close();
                }
            }
        });

        browser.getEngine().load(getAuthenticationURL());

        stage.setScene(scene);
        stage.setTitle("Authorize SlideshowFX in Box");
        stage.showAndWait();

        if(!this.isAuthenticated()) throw new HostingConnectorException(HostingConnectorException.AUTHENTICATION_FAILURE);
    }

    protected Map<String, String> getURIParameters(final URI uri) {
        final Map<String, String> parameters = new HashMap<>();
        final String query = uri.getQuery();

        if(query != null && !query.isEmpty()) {
            final String[] queryParameters = query.split("&");

            for(String parameter : queryParameters) {
                final int equalSign = parameter.indexOf('=');
                final String name = equalSign == -1 ? parameter : parameter.substring(0, equalSign);
                final String value = equalSign == -1 ? null : parameter.substring(equalSign + 1);

                parameters.put(name, value);
            }
        }

        return parameters;
    }

    protected String getAuthenticationURL() {
        final StringBuilder url = new StringBuilder("https://account.box.com/api/oauth2/authorize")
                .append("?response_type=code")
                .append("&client_id=").append(this.getOptions().getConsumerKey())
                .append("&redirect_uri=").append(this.getOptions().getRedirectUri())
                .append("&state=").append(System.currentTimeMillis());

        return url.toString();
    }

    @Override
    public boolean checkAccessToken() {
        boolean valid = false;

        if(this.boxApi != null) {
            try {
                this.boxApi.refresh();
            } catch(Exception e) {
                LOGGER.log(Level.FINE, "Error when trying to refresh the BOX api connection", e);
            }

            valid = this.accessToken != null && !this.accessToken.isEmpty()
                    && this.refreshToken != null && !this.refreshToken.isEmpty();
        }

        return valid;
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void upload(PresentationEngine engine, RemoteFile folder, boolean overwrite) throws HostingConnectorException, FileNotFoundException {
        if(engine == null) throw new NullPointerException("The engine can not be null");
        if(engine.getArchive() == null) throw new NullPointerException("The archive to upload can not be null");
        if(!engine.getArchive().exists()) throw new FileNotFoundException("The archive to upload does not exist");

        if(this.isAuthenticated()) {

        } else {
            throw new HostingConnectorException(HostingConnectorException.NOT_AUTHENTICATED);
        }
    }

    @Override
    public File download(File destination, RemoteFile file) throws HostingConnectorException {
        if(destination == null) throw new NullPointerException("The destination can not be null");
        if(file == null) throw new NullPointerException("The file to download can not be null");
        if(!destination.isDirectory()) throw new IllegalArgumentException("The destination is not a folder");

        File result;

        if(this.isAuthenticated()) {

        } else {
            throw new HostingConnectorException(HostingConnectorException.NOT_AUTHENTICATED);
        }

        return null;
    }

    @Override
    public List<RemoteFile> list(RemoteFile parent, boolean includeFolders, boolean includePresentations) throws HostingConnectorException {
        if(parent == null) throw new NullPointerException("The parent can not be null");

        final List<RemoteFile> folders = new ArrayList<>();

        if(this.isAuthenticated()) {

        } else {
            throw new HostingConnectorException(HostingConnectorException.NOT_AUTHENTICATED);
        }

        return folders;
    }

    @Override
    public boolean fileExists(PresentationEngine engine, RemoteFile destination) throws HostingConnectorException {
        if(engine == null) throw new NullPointerException("The engine can not be null");
        if(engine.getArchive() == null) throw new NullPointerException("The archive file can not be null");
        if(destination == null) throw new NullPointerException("The destination can not be null");

        boolean exist;

        if(this.isAuthenticated()) {

        } else {
            throw new HostingConnectorException(HostingConnectorException.NOT_AUTHENTICATED);
        }

        return false;
    }

}
