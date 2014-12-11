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

package com.twasyl.slideshowfx.uploader;

import com.twasyl.slideshowfx.engine.presentation.PresentationEngine;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * This interface defines an uploader that will enable to upload a presentation to a cloud based platform.
 *
 * @author Thierry Wasylczenko
 * @version 1.0
 * @since 1.0
 */
public interface IUploader {

    /**
     * The name of the uploader that will be used to identify it in the UI.
     * @return The name of the uploader.
     */
    String getName();

    /**
     * Indicates if the user is authenticated to the cloud based platform.
     * @return {@code true} if the user is authenticated, {@code false} otherwise.
     */
    boolean isAuthenticated();

    /**
     * Authenticate the user to the cloud based platform.
     * @return {@code true} if the user has been authenticated, {@code false} otherwise.
     */
    boolean authenticate();

    /**
     * Get the access token that is get when the user is successfully logged in the service.
     * @return The access token get by the authentication process.
     */
    String getAccessToken();

    /**
     * Disconnect the user from the service.
     */
    void disconnect();

    /**
     * Upload the given {@code engine} to the service. The presentation is uploaded to the root of the service.
     * @param engine The presentation to upload.
     * @throws java.lang.NullPointerException If the {@code engine} is {@code null} or if {@code engine.getArchive()}
     *                                          is {@code null}.
     * @throws java.io.FileNotFoundException If the archive file does not already exist.
     */
    void upload(PresentationEngine engine) throws FileNotFoundException;

    /**
     * Upload the given {@code engine} to the service. The presentation is uploaded in the given {@code folder}.
     * @param engine The presentation to upload.
     * @param folder The folder where the presented will be uploaded.
     * @throws java.lang.NullPointerException If the {@code engine} is {@code null} or if {@code engine.getArchive()}
     *                                          is {@code null}.
     * @throws java.io.FileNotFoundException If the archive file does not already exist.
     */
    void upload(PresentationEngine engine, File folder) throws FileNotFoundException;

    /**
     * List all folders already present on the cloud service.
     * @return The list of all folders present remotely.
     */
    List<File> getFolders();
}
