/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***********************************************************************/

package blue.origami.nezcc;

import java.io.IOException;

public class PythonParserGenerator extends ParserSourceGenerator {

	@Override
	protected void initSymbols() {
		this.defineSymbol("\t", "  ");
		this.defineSymbol("null", "None");
		this.defineSymbol("true", "True");
		this.defineSymbol("false", "False");
		this.defineSymbol("&&", "and");
		this.defineSymbol("||", "or");
		this.defineSymbol("!", "not");
		this.defineSymbol("this", "self");
		this.defineSymbol("{", ":");
		this.defineSymbol("}", "");
		this.defineSymbol(";", "");
		this.defineSymbol("{[", "[");
		this.defineSymbol("]}", "]");
		this.defineSymbol("function", "def");
		this.defineSymbol("lambda", "lambda");

		this.defineSymbol("TindexMap", "array");
		this.defineSymbol("TbyteSet", "array");
		this.defineSymbol("TfuncMap", "array");
		this.defineSymbol("Ipos", "0");
		this.defineSymbol("Iresult", "0");
	}

	@Override
	protected void writeHeader() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void writeFooter() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void declStruct(String typeName, String... fields) {
		String block = this.beginBlock();
		block = this.emitLine(block, "class %s %s", typeName, this.s("{"));
		this.incIndent();
		if (fields.length == 0) {
			block = this.emitLine(block, "def __init__(self) %s", this.s("{"));
		} else {
			block = this.emitLine(block, "def __init__(self%s%s) %s", this.s(","), this.emitParams(fields),
					this.s("{"));
		}
		this.incIndent();
		for (String f : fields) {
			String n = f.replace("?", "");
			String v = this.emitInit(f);
			block = this.emitLine(block, "self.%s = %s", n, v);
		}
		this.decIndent();
		block = this.emitLine(block, this.s("}"));
		this.decIndent();
		block = this.emitLine(block, this.s("}"));
		this.writeSection(this.endBlock(block));
	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {

	}

	@Override
	protected void declConst(String typeName, String constName, String literal) {
		this.writeSection(String.format("%s = %s", constName, literal));
	}

	@Override
	protected String emitApply(String func) {
		return String.format("%s(%s)", func, this.s("px"));
	}

	@Override
	protected String emitIf(String expr, String expr2, String expr3) {
		return String.format("(%s) if (%s) else (%s)", expr2, expr, expr3);
	}

	@Override
	protected String emitParserLambda(String match) {
		String lambda = String.format("%s px : (%s)", this.s("lambda"), match);
		String p = "p" + this.varSuffix();
		return lambda.replace("px", p);
	}

}
