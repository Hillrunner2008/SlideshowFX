/*
 * Copyright 2015 Thierry Wasylczenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twasyl.slideshowfx.markup.textile.activator;

import com.twasyl.slideshowfx.markup.IMarkup;
import com.twasyl.slideshowfx.markup.textile.TextileMarkup;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Hashtable;

/**
 * @author Thierry Wasylczenko
 */
public class TextileActivator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        Hashtable<String, String> props = new Hashtable<>();

        bundleContext.registerService(IMarkup.class.getName(), new TextileMarkup(), props);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }
}
