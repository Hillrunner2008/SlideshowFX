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

package com.twasyl.slideshowfx.utils.concurrent.actions;

import javafx.scene.Node;
import javafx.scene.control.MenuItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Thierry Wasylczenko
 */
public class DisableAction implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(DisableAction.class.getName());
    private final List<Object> elements = new ArrayList<>();

    private DisableAction() {
    }

    public static DisableAction forElement(final Node node) {
        final DisableAction action = new DisableAction();
        return action.and(node);
    }

    public static DisableAction forElement(final MenuItem menuItem) {
        final DisableAction action = new DisableAction();
        return action.and(menuItem);
    }

    public static DisableAction forElements(final Collection elements) {
        final DisableAction action = new DisableAction();
        return action.and(elements);
    }

    public DisableAction and(final Node node) {
        this.elements.add(node);
        return this;
    }

    public DisableAction and(final MenuItem menuItem) {
        this.elements.add(menuItem);
        return this;
    }

    public DisableAction and(final Collection elements) {
        this.elements.addAll(elements);
        return this;
    }

    @Override
    public void run() {
        this.elements.forEach(element -> {
            try {
                final Method setDisable = element.getClass().getMethod("setDisable", boolean.class);
                setDisable.invoke(element, false);
            } catch (NoSuchMethodException e) {
                LOGGER.log(Level.FINE, "No setDisableMethod found", e);
            } catch (InvocationTargetException e) {
                LOGGER.log(Level.WARNING, "Can not disable element", e);
            } catch (IllegalAccessException e) {
                LOGGER.log(Level.WARNING, "Can not disable element", e);
            }
        });
    }
}
