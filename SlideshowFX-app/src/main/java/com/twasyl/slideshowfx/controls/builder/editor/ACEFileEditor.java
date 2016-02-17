package com.twasyl.slideshowfx.controls.builder.editor;

import com.twasyl.slideshowfx.utils.ResourceHelper;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.twasyl.slideshowfx.global.configuration.GlobalConfiguration.getDefaultCharset;

/**
 * This file editor uses ACE in order to display the content of files. ACE provides
 * syntax highlighting and other options.
 *
 * @author Thierry Wasylczenko
 * @version 1.0
 * @since SlideshowFX 1.0.0
 */
public class ACEFileEditor extends AbstractFileEditor<WebView> {

    private static final Logger LOGGER = Logger.getLogger(ACEFileEditor.class.getName());

    public ACEFileEditor() {
        super();

        final WebView webView = new WebView();
        webView.getEngine().load(ResourceHelper.getExternalForm("/com/twasyl/slideshowfx/html/ace-file-editor.html"));

        this.setFileContent(webView);
    }

    public ACEFileEditor(File file) {
        this();
        this.setFile(file);
    }

    @Override
    public void updateFileContent() {
        if(getFile() == null) throw new NullPointerException("The fileProperty is null");

        try(final FileInputStream fileInput = new FileInputStream(getFile());
            final InputStreamReader inputReader = new InputStreamReader(fileInput, getDefaultCharset());
            final BufferedReader reader = new BufferedReader(inputReader)) {
            final StringBuilder builder = new StringBuilder();

            reader.lines().forEach(line -> builder.append(line).append("\n"));

            getFileContent().getEngine().getLoadWorker().stateProperty().addListener((value, oldState, newState) -> {
                if(newState == Worker.State.SUCCEEDED) {
                    getFileContent().getEngine().executeScript("setContent(\"".concat(Base64.getEncoder().encodeToString(builder.toString().getBytes())).concat("\");"));

                    // Set the mode of the editor depending on the file MIME type
                    String mode = "ace/mode/";
                    try {
                        final String mimeType = Files.probeContentType(getFile().toPath());

                        // If no mime type, fallback on the extension
                        if (mimeType == null) {
                            if(getFile().getName().endsWith(".js")) mode = mode.concat("javascript");
                            else if(getFile().getName().endsWith(".html"))mode = mode.concat("html");
                            else if(getFile().getName().endsWith(".css")) mode = mode.concat("css");
                            else mode = mode.concat("plain_text");
                        } else if(mimeType.contains("text/html")) mode = mode.concat("html");
                        else if(mimeType.contains("text/css")) mode = mode.concat("css");
                    } catch (IOException e) {
                        LOGGER.log(Level.FINE, "Can not determine MIME type for the file editor", e);
                        mode = mode.concat("plain_text");
                    }

                    getFileContent().getEngine().executeScript("setMode(\"".concat(mode).concat("\");"));
                }
            });

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can not save the content", e);
        }
    }

    @Override
    public void saveContent() {
        if(getFile() == null) throw new NullPointerException("The fileProperty is null");

        try(final FileWriter writer = new FileWriter(getFile())) {
            final String content = (String) this.getFileContent().getEngine().executeScript("getContent();");
            byte[] bytes = Base64.getDecoder().decode(content);

            writer.write(new String(bytes, getDefaultCharset()));
            writer.flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can not save the content", e);
        }
    }
}
