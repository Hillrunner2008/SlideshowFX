package com.twasyl.slideshowfx.hosting.connector.box;

import com.twasyl.slideshowfx.hosting.connector.BasicHostingConnectorOptions;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Box options for the Box hosting connector.
 *
 * @author Thierry Wasylczenko
 * @since SlideshowFX @@NEXT-VERSION@@
 * @version 1.0
 */
public class BoxHostingConnectorOptions extends BasicHostingConnectorOptions {
    private final StringProperty refreshToken = new SimpleStringProperty();

    public StringProperty refreshTokenProperty() { return this.refreshToken; }
    public String getRefreshToken() { return this.refreshToken.get(); }
    public void setRefreshToken(String refreshToken) { this.refreshToken.set(refreshToken); }
}
