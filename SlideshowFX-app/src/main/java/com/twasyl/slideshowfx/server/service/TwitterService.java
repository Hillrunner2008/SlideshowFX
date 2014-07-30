package com.twasyl.slideshowfx.server.service;

import com.twasyl.slideshowfx.beans.chat.ChatMessage;
import com.twasyl.slideshowfx.beans.chat.ChatMessageSource;
import com.twasyl.slideshowfx.beans.chat.ChatMessageStatus;
import com.twasyl.slideshowfx.server.SlideshowFXServer;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class allow to use Twitter in the chat.
 *
 * @author Thierry Wasylczenko
 * @version 1.0
 * @since 1.0
 */
public class TwitterService extends Verticle {
    private static final Logger LOGGER = Logger.getLogger(TwitterService.class.getName());

    private Configuration twitterConfiguration;
    private Twitter twitter = null;
    private TwitterStream twitterStream = null;

    private final ObjectProperty<RequestToken> requestToken = new SimpleObjectProperty<>();
    private final ObjectProperty<AccessToken> accessToken = new SimpleObjectProperty<>();

    @Override
    public void start() {
        final Map twitter = this.vertx.sharedData().getMap(SlideshowFXServer.SHARED_DATA_TWITTER);
        final String hashtag = (String) twitter.get(SlideshowFXServer.SHARED_DATA_TWITTER_HASHTAG);

        this.twitterConfiguration = new ConfigurationBuilder()
                .setOAuthConsumerKey("5luxVGxswd42RgTfbF02g")
                .setOAuthConsumerSecret("winWDhMbeJZ4m66gABqpohkclLDixnyeOINuVtPWs")
                .build();

        if(hashtag != null && !hashtag.isEmpty()) {
            this.connect();
            this.accessToken.addListener((value, oldValue, newValue) -> {
                if (newValue != null) {
                    FilterQuery query = new FilterQuery();
                    query.track(new String[]{hashtag});

                    this.twitterStream = new TwitterStreamFactory(this.twitterConfiguration).getInstance(this.accessToken.get());
                    this.twitterStream.addListener(this.buildTwitterStreamListener());
                    this.twitterStream.filter(query);
                }
            });
        }
    }

    @Override
    public void stop() {
        super.stop();
        if(this.twitterStream != null) {
            try {
                this.twitterStream.shutdown();
            } catch(Exception e) {
                LOGGER.log(Level.SEVERE, "Can not stop the Twitter stream", e);
            }
        }
    }

    private void connect() {
        this.twitter = TwitterFactory.getSingleton();

        try {
            this.twitter.setOAuthConsumer(this.twitterConfiguration.getOAuthConsumerKey(),
                    this.twitterConfiguration.getOAuthConsumerSecret());
        } catch(IllegalStateException e) {
            LOGGER.fine("Consumer keys alreay set up");
        }

        try {
            this.requestToken.set(twitter.getOAuthRequestToken());
            final String authUrl = this.requestToken.get().getAuthorizationURL();

            Platform.runLater(() -> {

                final WebView twitterBrowser = new WebView();
                final Scene scene = new Scene(twitterBrowser);
                final Stage stage = new Stage();

                twitterBrowser.getEngine().load(authUrl);

                twitterBrowser.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> observableValue, Worker.State state, Worker.State state2) {
                        if (state2 == Worker.State.SUCCEEDED) {
                            if (twitterBrowser.getEngine().getDocument().getDocumentURI().equals("https://api.twitter.com/oauth/authorize")) {
                                String pinCode = twitterBrowser.getEngine().getDocument().getElementsByTagName("kbd").item(0).getTextContent();

                                try {
                                    TwitterService.this.accessToken.set(twitter.getOAuthAccessToken(requestToken.get(), pinCode));
                                    twitter.verifyCredentials();
                                } catch (TwitterException e) {
                                    e.printStackTrace();
                                }

                                stage.close();
                            }
                        }
                    }
                });

                stage.setScene(scene);
                stage.show();
            });
        } catch (TwitterException | IllegalStateException e) {
            LOGGER.fine("Seems to be already connected to Twitter");
        }
    }

    private StatusListener buildTwitterStreamListener() {
        final StatusListener listener = new StatusListener() {
            @Override
            public void onStatus(Status status) {
                final ChatMessage chatMessage = new ChatMessage();
                chatMessage.setId(System.currentTimeMillis() + "");
                chatMessage.setSource(ChatMessageSource.TWITTER);
                chatMessage.setStatus(ChatMessageStatus.NEW);
                chatMessage.setAuthor("@" + status.getUser().getScreenName());
                chatMessage.setContent(status.getText());

                final JsonObject jsonTweet = new JsonObject(chatMessage.toJSON());

                TwitterService.this.vertx.eventBus().publish("slideshowfx.chat.attendee.message.add",jsonTweet);
                TwitterService.this.vertx.eventBus().publish("slideshowfx.chat.presenter.message.add",jsonTweet);
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

            }

            @Override
            public void onTrackLimitationNotice(int i) {

            }

            @Override
            public void onScrubGeo(long l, long l1) {

            }

            @Override
            public void onStallWarning(StallWarning stallWarning) {

            }

            @Override
            public void onException(Exception e) {
                e.printStackTrace();
            }
        };

        return listener;
    }
}
