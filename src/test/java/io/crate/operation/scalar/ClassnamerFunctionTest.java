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

package io.crate.operation.scalar;

import io.crate.analyze.symbol.Function;
import io.crate.analyze.symbol.Literal;
import io.crate.analyze.symbol.Symbol;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.Functions;
import io.crate.operation.Input;
import io.crate.operation.tablefunctions.TableFunctionModule;
import io.crate.plugin.ExamplePlugin;
import io.crate.types.DataType;
import io.crate.types.StringType;
import net.rtme.Classnamer;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ClassnamerFunctionTest {

    static {
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(new ConsoleAppender(
                new PatternLayout("[%d{ISO8601}][%-5p][%-25c] %m%n")));
    }

    protected ClassnamerFunction classnamerFunction;

    private void registerPlugin(Settings settings) throws Exception {
        ScalarFunctionModule scalarFunctionModule = new ScalarFunctionModule();
        TableFunctionModule tableFunctionModule = new TableFunctionModule();

        ExamplePlugin examplePlugin = new ExamplePlugin(settings);
        examplePlugin.onModule(scalarFunctionModule);

        Functions functions = new ModulesBuilder()
                .add(scalarFunctionModule)
                .add(tableFunctionModule)
                .createInjector()
                .getInstance(Functions.class);
        classnamerFunction = (ClassnamerFunction) functions.get(
                new FunctionIdent(ClassnamerFunction.NAME, Collections.<DataType>emptyList())
        );
    }

    @Test
    public void testEvaluate() throws Exception {
        registerPlugin(Settings.EMPTY);

        BytesRef value = classnamerFunction.evaluate(new Input[0]);
        validateClassnamer(value.utf8ToString());
    }

    @Test
    public void testNormalizeDoNothingByDefault() throws Exception {
        // by default, normalize will do nothing in order to force evaluation on every row
        registerPlugin(Settings.EMPTY);

        Function function = new Function(classnamerFunction.info(), Collections.<Symbol>emptyList());

        Symbol symbol = classnamerFunction.normalizeSymbol(function);
        assertThat(symbol, instanceOf(Function.class));
        assertThat((Function) symbol, is(function));
    }

    @Test
    public void testNormalizeWithCustomSetting() throws Exception {
        // lets change the setting to force normalization, function will be executed only once
        // per SQL statement (same value for all rows)
        Settings.Builder builder = Settings.builder()
                .put(ExamplePlugin.EXECUTE_PER_ROW_SETTING, false);
        registerPlugin(builder.build());

        Function function = new Function(classnamerFunction.info(), Collections.<Symbol>emptyList());

        Symbol symbol = classnamerFunction.normalizeSymbol(function);
        assertThat(symbol, instanceOf(Literal.class));

        Literal literal = (Literal) symbol;
        assertThat(literal.valueType(), instanceOf(StringType.class));
        BytesRef value = (BytesRef) literal.value();

        validateClassnamer(value.utf8ToString());
    }

    public static void validateClassnamer(String generatedClassName) throws Exception {
        // validate that the generated class name was indeed a valid
        String[] parts = generatedClassName.split("(?<=.)(?=\\p{Lu})");

        assertThat(parts[0], isIn(Classnamer.PART_CANDIDATE_FIRST));
        assertThat(parts[1], isIn(Classnamer.PART_CANDIDATE_SECOND));
        assertThat(parts[2], isIn(Classnamer.PART_CANDIDATE_THIRD));
    }
}
