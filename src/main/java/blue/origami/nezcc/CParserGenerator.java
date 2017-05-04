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
		this.defineSymbol("\t", "  ");
		this.defineSymbol("null", "NULL");
		this.defineSymbol("true", "1");
		this.defineSymbol("false", "0");
		this.defineSymbol(".", "->");
		this.defineSymbol("{[", "{");
		this.defineSymbol("]}", "}");
		this.defineSymbol("const", "static const");
		this.defineSymbol("function", "static");

		//
		this.defineSymbol("Tmatched", "int");
		this.defineSymbol("Tpx", "struct NezParserContext*");
		this.defineSymbol("Tinputs", "const char*");
		this.defineSymbol("Tlength", "size_t");
		this.defineSymbol("Tpos", "const char*");
		this.defineSymbol("Ipos", "0");
		this.defineSymbol("TtreeLog", "struct TreeLog*");
		this.defineSymbol("Ttree", "void*");
		this.defineSymbol("Tstate", "void*");
		this.defineSymbol("Tmemos", "struct MemoEntry*");

		this.defineSymbol("TnewFunc", "TreeFunc");
		this.defineSymbol("TsetFunc", "TreeSetFunc");
		this.defineSymbol("Tf", "ParserFunc");

		this.defineSymbol("px.newTree", "px->newFunc");
		this.defineSymbol("px.setTree", "px->setFunc");

		this.defineSymbol("Tch", "char");
		this.defineSymbol("Tcnt", "int");
		this.defineSymbol("Tshift", "int");
		this.defineSymbol("TindexMap", "unsigned char[256]");
		this.defineSymbol("TbyteSet", "int[8]");

		this.defineSymbol("Top", "int");
		this.defineSymbol("Iop", "0");
		this.defineSymbol("Tlabel", "const char*");
		this.defineSymbol("Ttag", "const char*");
		this.defineSymbol("Tvalue", "const char*");
		this.defineSymbol("value.length", "(value+strlen(value))");
		this.defineSymbol("Tdata", "void*");

		this.defineSymbol("Tm", "struct MemoEntry*");
		this.defineSymbol("Tkey", "unsigned long long int");
		this.defineSymbol("TmemoPoint", "int");
		this.defineSymbol("Tresult", "int");
		this.defineSymbol("Iresult", "0");

		// defined
		this.defineSymbol("bitis", "bitis");
		this.defineSymbol("eof", "eof");
		this.defineSymbol("getbyte", "getbyte");
		this.defineSymbol("nextbyte", "nextbyte");
		this.defineSymbol("initMemo", "initMemo");
		this.defineSymbol("longkey", "longkey");
		this.defineSymbol("getMemo", "getMemo");

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
