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

import com.google.common.collect.Sets;
import io.crate.analyze.symbol.Function;
import io.crate.analyze.symbol.Literal;
import io.crate.analyze.symbol.Symbol;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.FunctionInfo;
import io.crate.metadata.Scalar;
import io.crate.metadata.TransactionContext;
import io.crate.data.Input;
import io.crate.types.DataType;
import io.crate.types.DataTypes;
import net.rtme.Classnamer;
import org.apache.lucene.util.BytesRef;

import java.util.Collections;

public class ClassnamerFunction extends Scalar<BytesRef, Void> {

    static final String NAME = "classnamer";

    private final static FunctionInfo INFO = new FunctionInfo(
        new FunctionIdent(NAME, Collections.<DataType>emptyList()),
        DataTypes.STRING,
        FunctionInfo.Type.SCALAR,
        Sets.immutableEnumSet(FunctionInfo.Feature.COMPARISON_REPLACEMENT)
    );

    public static void register(ScalarFunctionModule module, boolean executePerRow) {
        module.register(new ClassnamerFunction(executePerRow));
    }

    private final boolean executePerRow;

    private ClassnamerFunction(boolean executePerRow) {
        this.executePerRow = executePerRow;
    }

    @Override
    public BytesRef evaluate(Input[] args) {
        return new BytesRef(Classnamer.generate());
    }

    @Override
    public FunctionInfo info() {
        return INFO;
    }

    @Override
    public Symbol normalizeSymbol(Function symbol, TransactionContext context) {
        if (!executePerRow) {
            return Literal.of(evaluate(new Input[0]));
        }
        /* There is no evaluation here, so the function is executed
           per row. Else every row would contain the same random value*/
        return symbol;
    }
}
