/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.plugin;

import io.crate.Plugin;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.FunctionImplementation;
import io.crate.operation.scalar.IsEvenScalarFunction;
import io.crate.types.DataTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.settings.Settings;

import java.util.Collection;
import java.util.Collections;

public class ExamplePlugin implements Plugin {
    public static final String EXECUTE_PER_ROW_SETTING = "plugin.example.executeScalarPerRow";
    private static final Logger LOGGER = LogManager.getLogger(ExamplePlugin.class);
    private final boolean executePerRow;

    public ExamplePlugin(Settings settings) {
        this.executePerRow = settings.getAsBoolean("plugin.example.executeScalarPerRow", Boolean.valueOf(true)).booleanValue();
        LOGGER.info("ExamplePlugin loaded, execute scalar function per row: {}", Boolean.valueOf(this.executePerRow));
    }

    public String name() {
        return "example-plugin";
    }

    public String description() {
        return "A example plugin demonstrating crate's plugin infrastructure.";
    }

    @Override
    public Collection<Module> createGuiceModules() {
        LOGGER.info("Registering new scalar function using MapBinder");
        return Collections.singletonList(new AbstractModule() {
            @Override
            protected void configure() {
                MapBinder<FunctionIdent, FunctionImplementation> functionBinder =
                        MapBinder.newMapBinder(binder(), FunctionIdent.class, FunctionImplementation.class);
                functionBinder.addBinding(new FunctionIdent(
                                                  IsEvenScalarFunction.NAME,
                                                  Collections.singletonList(DataTypes.LONG))
                                         ).toInstance(new IsEvenScalarFunction());
            }
        });
    }
}