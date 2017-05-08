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

public class CParserGenerator extends ParserSourceGenerator {

	@Override
	protected void initSymbols() {
		this.useUnsignedByte(true);
		this.defineSymbol("\t", "  ");
		this.defineSymbol("null", "NULL");
		this.defineSymbol("true", "1");
		this.defineSymbol("false", "0");
		this.defineSymbol(".", "->");
		this.defineSymbol("const", "static const");
		this.defineSymbol("function", "static");
		this.defineSymbol("switch", "switch");
		this.defineSymbol("while", "while");

		//
		this.defineVariable("matched", "int");
		this.defineVariable("px", "struct NezParserContext*");
		this.defineVariable("inputs", "const unsigned char*");
		this.defineVariable("length", "size_t");
		this.defineVariable("pos", "const unsigned char*");
		this.defineVariable("treeLog", "struct TreeLog*");
		this.defineVariable("tree", "void*");
		this.defineVariable("state", "void*");
		this.defineVariable("memos", "struct MemoEntry*");

		this.defineVariable("newFunc", "TreeFunc");
		this.defineVariable("setFunc", "TreeSetFunc");
		this.defineVariable("f", "ParserFunc");

		this.defineVariable("ch", "unsigned char");
		this.defineVariable("cnt", "int");
		this.defineVariable("shift", "int");
		this.defineVariable("indexMap", "unsigned char[256]");
		this.defineVariable("byteSet", "int[8]");

		this.defineVariable("op", "int");
		this.defineVariable("label", "const char*");
		this.defineVariable("tag", "const char*");
		this.defineVariable("value", "const char*");
		this.defineSymbol("value.length", "((const unsigned char*)value+strlen(value))");
		this.defineVariable("data", "const void*");

		this.defineVariable("m", "struct MemoEntry*");
		this.defineVariable("key", "unsigned long long int");
		this.defineVariable("memoPoint", "int");
		this.defineVariable("result", "int");

		// defined
		this.defineSymbol("bitis", "bitis");
		this.defineSymbol("neof", "neof");
		this.defineSymbol("getbyte", "getbyte");
		this.defineSymbol("nextbyte", "nextbyte");
		this.defineSymbol("initMemo", "initMemo");
		this.defineSymbol("longkey", "longkey");
		this.defineSymbol("getMemo", "getMemo");
		this.defineSymbol("memcmp", "(memcmp(%s,%s,%s)==0)");
		this.defineSymbol("matchBytes", "matchBytes");

		// link freelist
		this.defineSymbol("UtreeLog", "unuseTreeLog");
	}

	@Override
	protected void writeHeader() throws IOException {
		this.open(this.getFileBaseName() + ".c");
		this.writeResource("/blue/origami/nezcc/cparser-libs.c");
	}

	@Override
	protected void writeFooter() throws IOException {
		this.writeResource("/blue/origami/nezcc/cparser-main.c");
		this.showResource("/blue/origami/nezcc/cparser-man.txt", "$cmd$", this.getFileBaseName());
	}

	@Override
	protected void declStruct(String typeNameA, String... fields) {
		String block = this.beginBlock();
		String typeName = typeNameA.replace("*", "").replace("struct ", "");
		block = this.emitLine(block, "%s %s %s", this.s("struct"), typeName, this.s("{"));
		this.incIndent();
		for (String f : fields) {
			String n = f.replace("?", "");
			block = this.emitLine(block, "%s %s%s", this.T(n), n, this.s(";"));
		}
		this.decIndent();
		block = this.emitLine(block, "};");
		this.defineSymbol(typeNameA, "new" + typeName);
		block = this.emitLine(block, this.formatSignature(typeNameA, "new" + typeName, fields) + "{");
		this.incIndent();
		block = this.emitLine(block, "%s this = (%s)_malloc(sizeof(struct %s));", typeNameA, typeNameA, typeName);
		for (String f : fields) {
			String n = f.replace("?", "");
			String v = this.emitInit(f);
			block = this.emitLine(block, "this->%s = %s;", n, v);
		}
		block = this.emitLine(block, "return this;", typeNameA);
		this.decIndent();
		block = this.emitLine(block, this.s("}"));
		this.writeSection(this.endBlock(block));
	}

	@Override
	protected String arrayName(String typeName, String constName) {
		int loc = typeName.indexOf('[');
		if (loc >= 0) {
			return constName + typeName.substring(loc);
		}
		return constName;
	}

	@Override
	protected String arrayType(String typeName, String constName) {
		int loc = typeName.indexOf('[');
		if (loc >= 0) {
			return typeName.substring(0, loc);
		}
		return typeName;
	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {
		this.writeSection(String.format("typedef %s (*%s)(%s);", ret, typeName, this.emitParams(params)));
	}

	@Override
	protected void declProtoType(String ret, String funcName, String[] params) {
		this.writeSection(String.format("%s %s", this.formatSignature(ret, funcName, params), this.s(";")));
	}

}
