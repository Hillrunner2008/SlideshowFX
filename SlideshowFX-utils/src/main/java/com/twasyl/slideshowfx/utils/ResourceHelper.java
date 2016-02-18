package com.twasyl.slideshowfx.utils;

import com.twasyl.slideshowfx.utils.io.DefaultCharsetReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides utility methods for reading resource's content.
 *
 * @author Thierry Wasylczenko
 * @version 1.0
 * @since SlideshowFX 1.0.0
 */
public class ResourceHelper {
    private static final Logger LOGGER = Logger.getLogger(ResourceHelper.class.getName());

    /**
     * This method reads the content of the resource identified by its URL and return it in a String.
     * @param url The URL of the resource to read the content.
     * @return The String representing the content of the resource
     */
    public static String readResource(String url) {
        final StringBuilder builder = new StringBuilder();

        try(final BufferedReader reader = new DefaultCharsetReader(ResourceHelper.class.getResourceAsStream(url))) {
            reader.lines().forEach(line -> builder.append(line).append("\n"));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can not read the resource content", e);
        }

        return builder.toString();
    }

    /**
     * This method get the external form of the given {@code url}.
     * @param url The URL of the resource to get the external form.
     * @return The string representing the external form of the given {@code url}.
     */
    public static String getExternalForm(String url) {
        return ResourceHelper.class.getResource(url).toExternalForm();
    }

    /**
     * This method get the InputStream for the given internal resource {@code url}.
     * @param url The URL of the resource to get the InputStream.
     * @return The InputStream for the given {@code url}.
     */
    public static InputStream getInputStream(String url) { return ResourceHelper.class.getResourceAsStream(url); }

    /**
     * Get the URL object associated to the given resource.
     * @param url The URL resource to get the URL object from.
     * @return The URL object associated to {@code url}.
     */
    public static URL getURL(String url) { return ResourceHelper.class.getResource(url); }
}
