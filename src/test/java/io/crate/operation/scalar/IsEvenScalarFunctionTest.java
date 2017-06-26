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

import io.crate.action.sql.SessionContext;
import io.crate.data.Input;
import io.crate.metadata.Functions;
import io.crate.metadata.TransactionContext;
import io.crate.operation.tablefunctions.TableFunctionModule;
import io.crate.plugin.ExamplePlugin;
import io.crate.types.DataTypes;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class IsEvenScalarFunctionTest {

    private final TransactionContext transactionContext = new TransactionContext(SessionContext.SYSTEM_SESSION);

    static {
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(new ConsoleAppender(
                new PatternLayout("[%d{ISO8601}][%-5p][%-25c] %m%n")));
    }

    private IsEvenScalarFunction isEvenFunction;

    private void registerPlugin(Settings settings) throws Exception {
        ExamplePlugin examplePlugin = new ExamplePlugin(settings);

        Functions functions = new ModulesBuilder()
                .add(new ScalarFunctionModule())
                .add(new TableFunctionModule())
                .add(examplePlugin.createGuiceModules().iterator().next())
                .createInjector()
                .getInstance(Functions.class);
        isEvenFunction = (IsEvenScalarFunction) functions.getBuiltin(
                IsEvenScalarFunction.NAME, Collections.singletonList(DataTypes.LONG)
        );
    }

    @Test
    public void testEvaluate() throws Exception {
        registerPlugin(Settings.EMPTY);

        long evenNumber = 999999998;
        long oddNumber = 888888883;
        long signedEvenNumber = -4;
        long signedOddNumber = -5;

        assertTrue(isEvenFunction.evaluate((Input<Long>) () -> evenNumber));
        assertFalse(isEvenFunction.evaluate((Input<Long>) () -> oddNumber));
        assertTrue(isEvenFunction.evaluate((Input<Long>) () -> signedEvenNumber));
        assertFalse(isEvenFunction.evaluate((Input<Long>) () -> signedOddNumber));
    }

    @Test
    public void testInvalidNumberOfArguments() throws Exception {
        registerPlugin(Settings.EMPTY);

        long evenNumber = 999999998;
        long oddNumber = 888888883;

        try {
            isEvenFunction.evaluate();
            fail();
        }
        catch (IllegalArgumentException e) {
            assertEquals("Wrong number of arguments", e.getMessage());
        }

        try {
            isEvenFunction.evaluate((Input<Long>) () -> evenNumber, (Input<Long>) () -> oddNumber);
            fail();
        }
        catch (IllegalArgumentException e) {
            assertEquals("Wrong number of arguments", e.getMessage());
        }
    }
}
