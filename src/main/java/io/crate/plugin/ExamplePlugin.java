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
import io.crate.operation.scalar.ClassnamerFunction;
import io.crate.operation.scalar.ScalarFunctionModule;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

public class ExamplePlugin implements Plugin {

    public static final String EXECUTE_PER_ROW_SETTING = "plugin.example.executeScalarPerRow";
    private static final Logger LOGGER = Loggers.getLogger(ExamplePlugin.class);

    private final boolean executePerRow;

    public ExamplePlugin(Settings settings) {
        executePerRow = settings.getAsBoolean(EXECUTE_PER_ROW_SETTING, true);
        LOGGER.info("ExamplePlugin loaded, execute scalar function per row: {}", executePerRow);
    }

    @Override
    public String name() {
        return "example-plugin";
    }

    @Override
    public String description() {
        return "A example plugin demonstrating crate's plugin infrastructure.";
    }

    /**
     * Each plugin can provide a hook on already loaded modules.
     * In this example we want to create a {@link io.crate.metadata.Scalar} function which will
     * be available via SQL statements. Scalar functions can be registered on the
     * {@link io.crate.operation.scalar.ScalarFunctionModule}, to do so we must implement
     * the relevant <tt>onModule(AnyModule module)</tt> method.
     */
    public void onModule(ScalarFunctionModule module) {
        ClassnamerFunction.register(module, executePerRow);
    }
}
