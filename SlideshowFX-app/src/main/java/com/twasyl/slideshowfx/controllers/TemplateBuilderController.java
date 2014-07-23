/*
 * Copyright 2014 Thierry Wasylczenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twasyl.slideshowfx.controllers;

import com.twasyl.slideshowfx.controls.Dialog;
import com.twasyl.slideshowfx.controls.builder.editor.ConfigurationFileEditor;
import com.twasyl.slideshowfx.controls.builder.editor.IFileEditor;
import com.twasyl.slideshowfx.controls.builder.editor.ImageFileEditor;
import com.twasyl.slideshowfx.controls.builder.editor.SimpleFileEditor;
import com.twasyl.slideshowfx.controls.tree.FileTreeCell;
import com.twasyl.slideshowfx.controls.tree.TemplateTreeView;
import com.twasyl.slideshowfx.engine.template.TemplateEngine;
import com.twasyl.slideshowfx.io.SlideshowFXExtensionFilter;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class used for the Template Builder.
 *
 * @author Thierry Wasylczenko
 * @version 1.0
 * @since 1.0
 */
public class TemplateBuilderController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(TemplateBuilderController.class.getName());

    @FXML private SplitMenuButton saveButton;
    @FXML private SplitMenuButton buildButton;
    @FXML private SplitMenuButton addFolderButton;
    @FXML private TemplateTreeView templateContentTreeView;
    @FXML private TabPane openedFiles;

    private Stage stage;
    private TemplateEngine templateEngine;

    /**
     * Get the stage where this TemplateBuilder is in.
     * @return The stage where this TemplateBuilder is in.
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Set the stage where this TemplateBuilder will be in.
     * @param stage The new stage where the TemplateBuilder will be in.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * This method opens a dialog box only allowing the selection of directories.
     *
     * @param event The event associated to button clicked to call this method.
     */
    @FXML private void addFolderToTreeView(ActionEvent event) {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Add content");
        final File directory = chooser.showDialog(null);
        
        if(directory != null) {
            this.addContentToTreeView(directory);
        }
    }

    /**
     * This method opens a dialog box only allowing the selection of files.
     *
     * @param event The event associated to button clicked to call this method.
     */
    @FXML private void addFileToTreeView(ActionEvent event) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Add content");
        final File file = chooser.showOpenDialog(null);

        if(file != null) {
            this.addContentToTreeView(file);
        }
    }

    /**
     * Build the current template archive. This method checks if the archive has already been saved
     * or not. If so, the archive will be overwritten otherwise a dialog asks the user where to save it.
     *
     * @param event The event associated to button clicked to call this method.
     */
    @FXML private void buildTemplateArchive(ActionEvent event) {
        File destination = this.templateEngine.getArchive();

        if(destination == null) {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(SlideshowFXExtensionFilter.TEMPLATE_FILTER);
            destination = chooser.showSaveDialog(null);
        }

        if(destination != null) {
            this.templateEngine.setArchive(destination);
            try {
                this.templateEngine.saveArchive();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Can not save the template", e);
            }
        }
    }

    /**
     * Build the current template archive.
     *
     * @param event The event associated to button clicked to call this method.
     */
    @FXML private void buildAsTemplateArchive(ActionEvent event) {

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(SlideshowFXExtensionFilter.TEMPLATE_FILTER);
        File destination = chooser.showSaveDialog(null);

        if(destination != null) {
            this.templateEngine.setArchive(destination);
            try {
                this.templateEngine.saveArchive();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Can not save the template", e);
            }
        }
    }

    /**
     * Save the current opened file.
     * @param event The event associated to the click
     */
    @FXML private void saveCurrentFile(ActionEvent event) {
        IFileEditor currentFile = (IFileEditor) this.openedFiles.getSelectionModel().getSelectedItem();

        if(currentFile != null) {
            currentFile.saveContent();
        }
    }

    /**
     * Save all opened files.
     * @param event The event associated to the click
     */
    @FXML private void saveAllFiles(ActionEvent event) {
        this.openedFiles.getTabs()
                .stream()
                .filter(tab -> tab instanceof IFileEditor)
                .map(tab -> (IFileEditor) tab)
                .forEach(editor -> editor.saveContent());
    }

    /**
     * Delete the selection from the TreeView and the filesystem.
     *
     * @param event The event associated to button clicked to call this method.
     */
    @FXML private void deleteFromTreeView(ActionEvent event) {
        ObservableList<TreeItem<File>> selectedItems = this.templateContentTreeView.getSelectionModel().getSelectedItems();
        selectedItems.filtered(item -> item != this.templateContentTreeView.getRoot())
                     .forEach(item -> {
                         try {
                             this.templateContentTreeView.deleteContentOfTreeView(item);
                         } catch (IOException e) {
                             Dialog.showErrorDialog(null, "Error", "Can not delete the content");
                         }
                     });
    }

    /**
     * Allow the user to create a directory in the template. This method asks the user for the given directory name
     * (a value with slashes will create multiple directory) and creates the desired directory in the selection of the
     * TreeView. If there is no selection, the directory will be created at the root of this template.
     *
     * @param event The event associated to button clicked to call this method.
     */
    @FXML private void createDirectory(ActionEvent event) {
        final TextField field = new TextField();
        field.setPromptText("Directory name");

        Dialog.Response response = Dialog.showCancellableDialog(this.getStage(), "Create a directory", field);

        if(response == Dialog.Response.OK) {
            if(!field.getText().trim().isEmpty()) {
                TreeItem<File> parent = this.templateContentTreeView.getSelectionModel().getSelectedItem();

                if(parent == null) parent = this.templateContentTreeView.getRoot();
                else {
                    // Ensure the selected item contain a directory. If it contains a file, the parent is taken.
                    if(parent.getValue().isFile()) {
                        parent = parent.getParent();
                    }
                }

                /**
                 * Split the text by / and create each directory and append it to the TreeView.
                 */

                TreeItem<File> tmpItem;
                for(String name : field.getText().trim().split("/")) {
                    final File tmpFile = new File(parent.getValue(), name);
                    tmpItem = new TreeItem<>(tmpFile);

                    if(!tmpFile.exists()) tmpFile.mkdir();

                    // Avoid duplicates in the tree
                    Optional<TreeItem<File>> sameItem = parent.getChildren()
                            .stream()
                            .filter(item -> item.getValue().equals(tmpFile))
                            .findFirst();

                    if(!sameItem.isPresent()) {
                        parent.getChildren().add(tmpItem);
                        parent = tmpItem;
                    } else {
                        parent = sameItem.get();
                    }
                }
            }
        }
    }

    /**
     * This method adds the given content to the TreeView. It detects if a TreeItem containing a directory
     * is selected in the TreeView to add the content to the selection. If not, the content is added to the root
     * of the TreeView.
     *
     * @param content The content to add to the TreeView.
     */
    private void addContentToTreeView(File content) {
        if(content != null && content.exists()) {
            TreeItem<File> parent = this.templateContentTreeView.getSelectionModel().getSelectedItem();

            if(parent == null || !parent.getValue().isDirectory()) {
                parent = this.templateContentTreeView.getRoot();
            }

            this.templateContentTreeView.appendContentToTreeView(content, parent);
        }
    }

    /**
     * Get the template engine used for the builder.
     * @return The templated builder used for the builder.
     */
    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }

    /**
     * Set the new template engine for this builder. The engine must be fully initialized before calling this method
     * because it is used to initialize the view.
     *
     * @param templateEngine The template engine to be used.
     */
    public void setTemplateEngine(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;

        if(this.templateEngine != null) {

            final TreeItem root = new TreeItem(this.templateEngine.getWorkingDirectory());
            root.setExpanded(true);

            this.templateContentTreeView.setEngine(this.templateEngine);
            this.templateContentTreeView.setRoot(root);

            for(File file : this.templateEngine.getWorkingDirectory().listFiles()) {
                this.addContentToTreeView(file);
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.buildButton.setGraphic(
                new ImageView(
                        new Image(getClass().getResourceAsStream("/com/twasyl/slideshowfx/images/package.png"), 20d, 20d, true, true)
                )
        );

        this.saveButton.setGraphic(
                new ImageView(
                        new Image(getClass().getResourceAsStream("/com/twasyl/slideshowfx/images/save.png"), 20d, 20d, true, true)
                )
        );

        this.addFolderButton.setGraphic(
                new ImageView(
                        new Image(getClass().getResourceAsStream("/com/twasyl/slideshowfx/images/add_folder.png"), 20d, 20d, true, true)
                )
        );

        // Initialize the tree view
        this.templateContentTreeView.setOnItemClick(event -> {
            if(event.getClickCount() == 2 && event.getButton().equals(MouseButton.PRIMARY)) {
                if(event.getSource() instanceof FileTreeCell) {
                    File file = ((FileTreeCell) event.getSource()).getItem();

                    if(file.isFile()) {

                        /**
                         * Check if the file is already opened and select it if it is,
                          otherwise open it.
                         */
                        Optional<IFileEditor> editor =  this.openedFiles.getTabs()
                                .stream()
                                .filter(tab -> tab instanceof IFileEditor)
                                .map(tab -> (IFileEditor) tab)
                                .filter(tab -> tab.getFile().equals(file))
                                .findFirst();

                        if(editor.isPresent()) {
                            this.openedFiles.getSelectionModel().select((Tab) editor.get());
                        } else {

                            // The type of editor has to be determined
                            IFileEditor fileEditor;

                            // The file is the configuration file
                            if(file.equals(new File(this.templateEngine.getWorkingDirectory(), this.templateEngine.getConfigurationFilename()))) {
                                fileEditor = new ConfigurationFileEditor(this.templateEngine.getWorkingDirectory().toPath(), file);
                            } else {
                                /**
                                 * Try to determine the best file editor to use
                                 * by checking the MIME type
                                 */
                                try {
                                    String mimeType = Files.probeContentType(file.toPath());

                                    if(mimeType != null && mimeType.contains("image")) fileEditor = new ImageFileEditor();
                                    else fileEditor = new SimpleFileEditor();
                                } catch (IOException e) {
                                    LOGGER.log(Level.WARNING, "An error occurred while truing to determine the MIME type of the file to open", e);
                                    fileEditor = new SimpleFileEditor();
                                }
                            }

                            fileEditor.setWorkingPath(this.templateEngine.getWorkingDirectory().toPath());
                            fileEditor.setFile(file);
                            fileEditor.updateFileContent();

                            this.openedFiles.getTabs().add((Tab) fileEditor);
                            this.openedFiles.getSelectionModel().selectLast();
                        }
                    }
                }
            }
        });
    }
}
