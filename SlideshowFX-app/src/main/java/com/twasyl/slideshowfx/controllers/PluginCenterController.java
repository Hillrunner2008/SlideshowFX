package com.twasyl.slideshowfx.controllers;

import com.twasyl.slideshowfx.content.extension.IContentExtension;
import com.twasyl.slideshowfx.hosting.connector.IHostingConnector;
import com.twasyl.slideshowfx.io.SlideshowFXExtensionFilter;
import com.twasyl.slideshowfx.markup.IMarkup;
import com.twasyl.slideshowfx.osgi.OSGiManager;
import com.twasyl.slideshowfx.plugin.InstalledPlugin;
import com.twasyl.slideshowfx.snippet.executor.ISnippetExecutor;
import com.twasyl.slideshowfx.utils.DialogHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller of the {@code PluginCenter.fxml} view.
 *
 * @author Thierry Wasylczenko
 * @version 1.0
 * @since SlideshowFX @@NEXT-VERSION@@
 */
public class PluginCenterController implements Initializable {

    private static Logger LOGGER = Logger.getLogger(PluginCenterController.class.getName());

    private enum PluginAction {
        INSTALL, REMOVE, DO_NOT_CHANGE;
    }

    @FXML private TableView<InstalledPlugin> pluginsView;
    @FXML private Button installPlugin;

    private Map<InstalledPlugin, PluginAction> pluginsAction;

    @FXML
    private void dragEntersPluginButton(final DragEvent event) {
        final Dragboard dragboard = event.getDragboard();

        if(dragboard.hasFiles()) {
            System.out.println("has files");
            final File file = dragboard.getFiles().get(0);

            try {
                if(this.fileSeemsValid(file)) {
                    System.out.println("is valid");
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    event.consume();
                }
            } catch (FileNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Can not check validity of file", e);
            }
        }
        System.out.println("out");

        event.consume();
    }

    @FXML
    private void dragExitsPluginButton(final DragEvent event) {
        event.consume();
    }

    @FXML
    private void dropFileOverPluginButton(final DragEvent event) {
        boolean success = false;
        final Dragboard dragboard = event.getDragboard();

        if(dragboard.hasFiles()) {
            System.out.println("Drop - has files");
            final File pluginFile = dragboard.getFiles().get(0);

            success = this.checkChosenPluginFile(pluginFile);
        }

        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void choosePlugin(final ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(SlideshowFXExtensionFilter.PLUGIN_FILES);
        File pluginFile = chooser.showOpenDialog(null);

        if(pluginFile != null) {
            this.checkChosenPluginFile(pluginFile);
        }
    }

    /**
     * Checks if a file chosen by the user is valid or not. In case the file is not a valid plugin, then an error
     * message is displayed. If it is valid, the plugin file is added to the list of plugins to install and displayed
     * in the plugins table.
     * @param pluginFile The plugin file to check.
     * @return {@code true} if the file is a valid plugin, {@code false} otherwise.
     */
    protected boolean checkChosenPluginFile(final File pluginFile) {
        boolean valid = false;

        try {
            if(fileSeemsValid(pluginFile)) {
                final InstalledPlugin installedPlugin = this.createInstalledPlugin(pluginFile);

                this.pluginsAction.put(installedPlugin, PluginAction.INSTALL);
                this.pluginsView.getItems().add(installedPlugin);

                valid = true;
            }
            else {
                DialogHelper.showError("Invalid plugin", "The chosen plugin seems invalid");
            }
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Can not determine if the plugin file seems valid", e);
        }

        return valid;
    }

    /**
     * Checks if the given {@link File file} is a plugin seems to be a plugin that can be installed in SlideshowFX.
     * @param file The file to check.
     * @return {@code true} if the file seems to be plugin, {@code false} otherwise.
     * @throws NullPointerException If the file is {@code null}.
     * @throws FileNotFoundException If the file doesn't exist.
     */
    protected boolean fileSeemsValid(final File file) throws FileNotFoundException {
        if(file == null) throw new NullPointerException("The file to check can not be null");
        if(!file.exists()) throw new FileNotFoundException("The file to check must exist");

        boolean isValid = false;

        if(file.getName().endsWith(".jar")) {
            try(final JarFile jar = new JarFile(file)) {
                final Manifest manifest = jar.getManifest();
                final Attributes attributes = manifest.getMainAttributes();

                if(attributes != null) {
                    final String name = attributes.getValue("Bundle-Name");
                    final String version = attributes.getValue("Bundle-Version");
                    final String activator = attributes.getValue("Bundle-Activator");

                    isValid = isManifestAttributeValid(name) && isManifestAttributeValid(version)
                            && isManifestAttributeValid(activator);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Can not create a JarFile instance for the plugin: " + file.getName(), e);
            }
        }

        return isValid;
    }

    /**
     * Check if a given attribute is considered valid. An attribute is considered valid if is is not null and not
     * empty.
     * @param attribute The attribute to check.
     * @return {@code true} if the attribute is valid, {@code false} otherwise.
     */
    protected boolean isManifestAttributeValid(final String attribute) {
        return attribute != null && !attribute.trim().isEmpty();
    }

    protected InstalledPlugin createInstalledPlugin(final File pluginFile) {
        InstalledPlugin plugin = null;

        try(final JarFile jar = new JarFile(pluginFile)) {
            final Manifest manifest = jar.getManifest();
            final Attributes attributes = manifest.getMainAttributes();

            if(attributes != null) {
                final String name = attributes.getValue("Bundle-Name");
                final String version = attributes.getValue("Bundle-Version");

                plugin = new InstalledPlugin(name, version);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can not create a JarFile instance for the plugin: " + pluginFile.getName(), e);
        }

        return plugin;
    }

    protected void populatePluginsTable() {
        final List<InstalledPlugin> installedPlugins = OSGiManager.getInstalledPlugins(IMarkup.class);
        installedPlugins.addAll(OSGiManager.getInstalledPlugins(IContentExtension.class));
        installedPlugins.addAll(OSGiManager.getInstalledPlugins(ISnippetExecutor.class));
        installedPlugins.addAll(OSGiManager.getInstalledPlugins(IHostingConnector.class));

        installedPlugins.forEach(plugin -> this.pluginsAction.put(plugin, PluginAction.DO_NOT_CHANGE));

        this.pluginsView.getItems().addAll(installedPlugins);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.pluginsAction = new HashMap<>();

        this.populatePluginsTable();
    }
}
