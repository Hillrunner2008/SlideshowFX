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

package com.twasyl.slideshowfx.markup.asciidoctor;

import com.twasyl.slideshowfx.markup.AbstractMarkup;
import org.asciidoctor.Asciidoctor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the asciidoctor syntax.
 * This markup language is identified byt the code <code>ASCIIDOCTOR</code> which is returned by {@link com.twasyl.slideshowfx.markup.IMarkup#getCode()}.
 *
 * @author Thierry Wasylczenko
 * @since 1.0
 * @version 1.0
 */
public class AsciidoctorMarkup extends AbstractMarkup {

    private static final Logger LOGGER = Logger.getLogger(AsciidoctorMarkup.class.getName());
    private final Asciidoctor asciidoctor;

    private final List<String> loadPaths = new ArrayList<>();

    public AsciidoctorMarkup() {
        super("ASCIIDOCTOR", "asciidoctor");

        this.asciidoctor = Asciidoctor.Factory.create();
    }

    @Override
    public String convertAsHtml(String markupString) throws IllegalArgumentException {
        if(markupString == null) throw new IllegalArgumentException("Can not convert " + getName() + " to HTML : the String is null");

        return this.asciidoctor.render(markupString,new HashMap<String, Object>());
    }
}
