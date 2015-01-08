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

package com.twasyl.slideshowfx.app;

import com.leapmotion.leap.Controller;
import com.sun.javafx.PlatformUtil;
import com.sun.javafx.application.LauncherImpl;
import com.twasyl.slideshowfx.controls.SlideShowScene;
import com.twasyl.slideshowfx.hosting.connector.HostingConnectorManager;
import com.twasyl.slideshowfx.io.DeleteFileVisitor;
import com.twasyl.slideshowfx.leap.SlideshowFXLeapListener;
import com.twasyl.slideshowfx.osgi.OSGiManager;
import com.twasyl.slideshowfx.server.SlideshowFXServer;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SlideshowFX extends Application {

    static {
        /*
         * Set the path of LeapMotion libraries when the application is packaged. This is mostly useful when the app is
         * bundled as a Mac Bundle.
         *
         * We first look for a custom property "dynamic.java.library.path". If it is set to true, the hack will be performed.
         * This property is useful to be sure the app is working inside an IDE and in production. Indeed, the custom
         * property should only be used when the application is packaged.
         */
        final String defineDynamicJavaLibraryPath = System.getProperty("dynamic.java.library.path");

        if("true".equals(defineDynamicJavaLibraryPath)) {
            try {
                // Trick to get the app JAR file
                final File appJarFile = new File(SlideshowFX.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

                String platform = "";
                if(PlatformUtil.isMac()) platform = "osx";

                else if(PlatformUtil.isWindows()) {
                    if("64".equals(System.getProperty("sun.arch.data.model"))) platform = "windows_x64";
                    if("86".equals(System.getProperty("sun.arch.data.model"))) platform = "windows_x86";
                }

                else if(PlatformUtil.isLinux() || PlatformUtil.isUnix()) {
                    if("64".equals(System.getProperty("sun.arch.data.model"))) platform = "linux_x64";
                    if("86".equals(System.getProperty("sun.arch.data.model"))) platform = "linux_x86";
                }

                /*
                 * Once we know where the JAR is, we assume the libraries are located next to it in a "Leap" folder and then
                 * in a subfolder for each platform architecture.
                 */
                System.setProperty("java.library.path", new File(appJarFile.getParentFile(), "Leap/" + platform).getAbsolutePath());

                Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");

                if (fieldSysPath != null) {
                    fieldSysPath.setAccessible(true);
                    fieldSysPath.set(null, null);
                }
            } catch (URISyntaxException | IllegalAccessException | NoSuchFieldException e) {
                Logger.getLogger(SlideshowFX.class.getName()).severe("Impossible to set java.library.path for LeapMotion");
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(SlideshowFX.class.getName());
    private static final ReadOnlyObjectProperty<Stage> stage = new SimpleObjectProperty<>();
    private static final ReadOnlyObjectProperty<Scene> presentationBuilderScene = new SimpleObjectProperty<>();
    private static final ObjectProperty<SlideShowScene> slideShowScene = new SimpleObjectProperty<>();
    private static final BooleanProperty slideShowActive = new SimpleBooleanProperty(false);
    private static final BooleanProperty leapMotionAllowed = new SimpleBooleanProperty();

    private static Controller leapController;
    private static SlideshowFXLeapListener slideshowFXLeapListener;

    @Override
    public void init() throws Exception {
        // Init LeapMotion
        slideshowFXLeapListener = new SlideshowFXLeapListener();
        leapController = new Controller();

        // The listener is added and removed each time the slideShowActive property changes
        slideShowActiveProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                if (aBoolean2) {
                    leapController.addListener(slideshowFXLeapListener);
                } else {
                    leapController.removeListener(slideshowFXLeapListener);

                    getStage().close();

                    ((SimpleObjectProperty) stageProperty()).set(new Stage(StageStyle.DECORATED));
                    getStage().setScene(presentationBuilderScene.get());
                    getStage().setMaximized(true);
                    getStage().show();
                }
            }
        });

        // LeapMotion controller should track gestures if it is enabled by the application and the slideshow is active
        leapMotionAllowedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean aBoolean2) {
                slideshowFXLeapListener.setTracking(aBoolean2);
                LOGGER.finest(String.format("LeapMotion tracking has changed to %1$s", aBoolean2));
            }
        });

        // Init the slideshow scene
        slideShowSceneProperty().addListener(new ChangeListener<SlideShowScene>() {
            @Override
            public void changed(ObservableValue<? extends SlideShowScene> observableValue, SlideShowScene scene, SlideShowScene scene2) {
                if (scene2 != null) {
                    getStage().setScene(scene2);
                    getStage().setFullScreen(true);

                    SlideshowFX.setSlideShowActive(true);
                }
            }
        });

        // Start the MarkupManager
        LOGGER.info("Starting Felix");
        OSGiManager.startAndDeploy();
    }

    @Override
    public void start(Stage stage) throws Exception {
        ((SimpleObjectProperty<Stage>) SlideshowFX.stage).set(stage);

        final Parent root = FXMLLoader.load(getClass().getResource("/com/twasyl/slideshowfx/fxml/SlideshowFX.fxml"));

        final Scene scene = new Scene(root);
        ((SimpleObjectProperty<Scene>) presentationBuilderScene).set(scene);

        stage.setTitle("SlideshowFX");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.getIcons().addAll(
                new Image(SlideshowFX.class.getResourceAsStream("/com/twasyl/slideshowfx/images/appicons/16.png")),
                new Image(SlideshowFX.class.getResourceAsStream("/com/twasyl/slideshowfx/images/appicons/32.png")),
                new Image(SlideshowFX.class.getResourceAsStream("/com/twasyl/slideshowfx/images/appicons/64.png")),
                new Image(SlideshowFX.class.getResourceAsStream("/com/twasyl/slideshowfx/images/appicons/128.png")),
                new Image(SlideshowFX.class.getResourceAsStream("/com/twasyl/slideshowfx/images/appicons/256.png")),
                new Image(SlideshowFX.class.getResourceAsStream("/com/twasyl/slideshowfx/images/appicons/512.png")));
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        LOGGER.info("Cleaning temporary files");
        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));

        Arrays.stream(tempDirectory.listFiles())
              .filter(file -> { return file.getName().startsWith("sfx-"); })
              .forEach(file -> {
                  try {
                      Files.walkFileTree(file.toPath(), new DeleteFileVisitor());
                  } catch (IOException e) {
                      LOGGER.log(Level.SEVERE,
                              String.format("Can not delete temporary file %1$s", file.getAbsolutePath()),
                              e);
                  }
              });

        LOGGER.info("Stopping the LeapMotion controller correctly");
        leapController.removeListener(slideshowFXLeapListener);

        LOGGER.info("Closing the chat");
        if(SlideshowFXServer.getSingleton() != null) SlideshowFXServer.getSingleton().stop();

        LOGGER.info("Disconnecting from all hosting connectors");
        HostingConnectorManager.getInstalledHostingConnectors()
                .forEach(hostingConnector -> hostingConnector.disconnect());

        LOGGER.info("Stopping the OSGi manager");
        OSGiManager.stop();
    }

    public static ReadOnlyObjectProperty<Stage> stageProperty() { return stage; }
    public static Stage getStage() { return stageProperty().get(); }

    public static final BooleanProperty slideShowActiveProperty() { return slideShowActive; }
    public static final Boolean isSlideShowActive() { return slideShowActiveProperty().get(); }
    public static final void setSlideShowActive(boolean active) { slideShowActiveProperty().set(active); }

    public static BooleanProperty leapMotionAllowedProperty() { return leapMotionAllowed; }
    public static boolean isLeapMotionAllowed() { return leapMotionAllowedProperty().get(); }
    public static void setLeapMotionAllowed(boolean leapMotionAllowed) { leapMotionAllowedProperty().set(leapMotionAllowed); }

    public static final ObjectProperty<SlideShowScene> slideShowSceneProperty() { return slideShowScene; }
    public static final SlideShowScene getSlideShowScene() { return slideShowSceneProperty().get(); }
    public static final void setSlideShowScene(SlideShowScene scene) { slideShowSceneProperty().set(scene); }

    public static void main(String[] args) {
        LauncherImpl.launchApplication(SlideshowFX.class, SlideshowFXPreloader.class, args);
    }
}
