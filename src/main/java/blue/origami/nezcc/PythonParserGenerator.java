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
		this.useUnsignedByte(true);

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
		this.defineSymbol("lambda", "lambda px : %s");
		this.defineSymbol("else if", "elif");

		this.defineSymbol("px.newTree", "px.newFunc");
		this.defineSymbol("px.setTree", "px.setFunc");

		// this.defineVariable("funcMap", "array");
		this.defineSymbol("PnewFunc", "=None");
		this.defineSymbol("PsetFunc", "=None");
		this.defineSymbol("Cinputs0", "(%s + '\\0').encode('utf-8')");
		this.defineSymbol("inputs0.length", "len(inputs)-1");
	}

	@Override
	protected void writeHeader() throws IOException {
		this.open(this.getFileBaseName() + ".py");
	}

	@Override
	protected void writeFooter() throws IOException {

	}

	@Override
	protected void declStruct(String typeName, String... fields) {
		String block = this.beginBlock();
		block = this.emitLine(block, "class %s %s", typeName, this.s("{"));
		this.incIndent();
		String[] a = this.joins("self", fields);
		block = this.emitLine(block, "def __init__(%s) %s", this.emitParams(a), this.s("{"));
		this.incIndent();
		block = this.emitInits(block, fields);
		this.decIndent();
		block = this.emitLine(block, this.s("}"));
		this.decIndent();
		block = this.emitLine(block, this.s("}"));
		this.writeSection(this.endBlock(block));
	}

	@Override
	protected String emitIf(String expr, String expr2, String expr3) {
		return String.format("(%s) if (%s) else (%s)", expr2, expr, expr3);
	}

	@Override
	protected String emitNewArray(String type, String index) {
		return String.format("[None] * %s", index);
	}

	// Tree

	@Override
	protected void declTree() {
	}

	@Override
	protected String emitNewToken(String tag, String inputs, String pos, String epos) {
		return String.format("(%s, (%s[%s:%s]).decode('utf-8'))", tag, inputs, pos, epos);
	}

	@Override
	protected String emitNewTree(String tag, String nsubs) {
		return String.format("(%s, ([None] * %s))", tag, nsubs);
	}

	@Override
	protected String emitSetTree(String parent, String n, String label, String child) {
		String block = this.beginBlock();
		block = this.emitLine(block, "%s[1][%s] = (%s, %s)", parent, n, label, child);
		block = this.emitStmt(block, this.emitReturn(parent));
		return (this.endBlock(block));
	}

}
