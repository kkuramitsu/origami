/************************************************************************Copyright 2017 Kimio Kuramitsu and ORIGAMI project**Licensed under the Apache License,Version 2.0(the"License");*you may not use this file except in compliance with the License.*You may obtain a copy of the License at**http://www.apache.org/licenses/LICENSE-2.0
**Unless required by applicable law or agreed to in writing,software*distributed under the License is distributed on an"AS IS"BASIS,*WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.*See the License for the specific language governing permissions and*limitations under the License.***********************************************************************/

package blue.origami.nezcc;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import blue.origami.nez.peg.expression.ByteSet;

public class JavaParserGenerator extends ParserSourceGenerator {

	@Override
	protected void initSymbols() {
		this.defineSymbol("\t", " ");
		this.defineSymbol("lambda", "(px) -> %s");
		this.defineSymbol("const", "private static final");
		this.defineSymbol("function", "private static final");
		this.defineSymbol("while", "while");
		this.defineSymbol("switch", "switch");

		this.defineVariable("matched", "boolean");
		this.defineVariable("px", "NezParserContext");
		this.defineVariable("inputs", "byte[]");
		this.defineVariable("length", "int");
		this.defineVariable("pos", "int");
		this.defineVariable("treeLog", "TreeLog");
		this.defineVariable("tree", "Object");
		this.defineVariable("state", "State");
		this.defineVariable("memos", "MemoEntry[]");

		this.defineVariable("newFunc", "TreeFunc");
		this.defineVariable("setFunc", "TreeSetFunc");
		this.defineVariable("f", "ParserFunc");

		this.defineVariable("c", "int");
		this.defineVariable("cnt", "int");
		this.defineVariable("shift", "int");
		this.defineVariable("indexMap", "short[256]");
		this.defineSymbol("bitis", "bitis");
		if (this.isDefined("bitis")) {
			this.defineVariable("byteSet", "int[8]");
			this.defineVariable("s", "int[]");
		} else {
			this.defineVariable("byteSet", "boolean[256]");
			this.defineVariable("s", "boolean[]");
		}
		this.defineSymbol("matchBytes", "matchBytes");

		this.defineVariable("op", "int");
		this.defineVariable("label", "String");
		this.defineVariable("tag", "String");
		this.defineVariable("value", "byte[]");
		this.defineSymbol("len", "%s.length");
		this.defineVariable("data", "Object");

		this.defineVariable("m", "MemoEntry");
		this.defineVariable("key", "long");
		this.defineVariable("memoPoint", "int");
		this.defineVariable("result", "int");

		// this.defineSymbol("UtreeLog", "unuseTreeLog");
	}

	@Override
	protected void writeHeader() throws IOException {
		this.open(this.getFileBaseName() + ".java");
		this.writeResource("/blue/origami/nezcc/javaparser-imports.java");
		this.writeLine("");
		this.writeLine("class %s {", this.getFileBaseName());
		this.writeResource("/blue/origami/nezcc/javaparser-libs.java");
	}

	@Override
	protected void writeFooter() throws IOException {
		this.writeResource("/blue/origami/nezcc/javaparser-main.java");
		this.writeLine("}");
		this.showResource("/blue/origami/nezcc/javaparser-man.txt", "$cmd$", this.getFileBaseName());
	}

	@Override
	protected void declStruct(String typeName, String... fields) {
		StringBuilder block = this.beginBlock();
		this.emitStmt(block, String.format("static class %s {", typeName));
		this.incIndent();
		for (String f : fields) {
			f = f.replace("?", "");
			this.emitStmt(block, String.format("%s %s;", this.T(f), f));
		}
		/* Constructor */
		this.emitStmt(block, String.format("%s(%s) {", typeName.replace("<T>", ""), this.emitParams(fields)));
		this.incIndent();
		this.emitInits(block, fields);
		this.decIndent();
		this.defineSymbol(typeName, "new " + typeName.replace("<T>", "<>"));
		this.emitStmt(block, "}");
		this.decIndent();
		this.emitStmt(block, "}");
		this.writeSection(this.endBlock(block));
	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {
		StringBuilder block = this.beginBlock();
		this.emitStmt(block, String.format("public interface %s {", typeName));
		this.incIndent();
		this.emitStmt(block, String.format("%s apply(%s);", ret, this.emitParams(params)));
		this.decIndent();
		this.emitStmt(block, "}");
		this.writeSection(this.endBlock(block));
	}

	// @Override
	// protected String emitChar(int uchar) {
	// return "(byte)" + (byte) uchar;
	// }

	@Override
	protected String emitNewArray(String type, String index) {
		return String.format("new %s[%s]", type, index);
	}

	@Override
	protected String emitApply(String func, List<String> params) {
		return this.emitFunc(String.format("%s.apply", func), params);
	}

	@Override
	protected String emitFuncRef(String funcName) {
		return String.format("%s::%s", this.getFileBaseName(), funcName);
	}

	@Override
	protected String vIndexMap(byte[] indexMap) {
		byte[] encoded = Base64.getEncoder().encode(indexMap);
		return this.getConstName(this.T("indexMap"), encoded.length, "I(\"" + new String(encoded) + "\")");
	}

	@Override
	protected String vByteSet(ByteSet bs) {
		if (this.isDefined("bitis")) {
			return super.vByteSet(bs);
		}
		int last = 0;
		for (int i = 0; i < 256; i++) {
			if (bs.is(i)) {
				last = i;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("B(\"");
		for (int i = 0; i <= last; i++) {
			sb.append(bs.is(i) ? "1" : "0");
		}
		sb.append("\")");
		return this.getConstName(this.T("byteSet"), 256, sb.toString());
	}

	@Override
	protected String matchBytes(byte[] text, boolean proceed) {
		return String.format("matchBytes(px, %s)", this.vMultiBytes(text));
	}

	protected String vMultiBytes(byte[] text) {
		byte[] encoded = Base64.getEncoder().encode(text);
		return this.getConstName("byte[]", encoded.length, "B64(\"" + new String(encoded) + "\")");
	}

}
