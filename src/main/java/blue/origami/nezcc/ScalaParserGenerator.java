/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package blue.origami.nezcc;

import java.io.IOException;
import java.util.List;

import blue.nez.peg.expression.ByteSet;

public class ScalaParserGenerator extends ParserSourceGenerator {

	@Override
	protected void initSymbols() {
		this.defineSymbol("\t", "  ");
		this.defineSymbol("lambda", "(px) -> %s");
		this.defineSymbol("const", "private static final");
		this.defineSymbol("function", "private static final");
		this.defineSymbol("while", "while");
		this.defineSymbol("switch", "switch");

		this.defineVariable("matched", "Bool");
		this.defineVariable("px", "NezParserContext");
		this.defineVariable("inputs", "Array[Byte]");
		this.defineVariable("length", "Int");
		this.defineVariable("pos", "Int");
		this.defineVariable("treeLog", "TreeLog");
		this.defineVariable("tree", "Object");
		this.defineVariable("state", "Object");
		this.defineVariable("memos", "Array[MemoEntry]");

		this.defineVariable("newFunc", "TreeFunc");
		this.defineVariable("setFunc", "TreeSetFunc");
		this.defineVariable("f", "NezParserContext -> Bool");

		this.defineVariable("ch", "Byte");
		this.defineVariable("cnt", "Int");
		this.defineVariable("shift", "Int");
		this.defineVariable("indexMap", "Array[Short]");
		this.defineVariable("byteSet", "Array[Bool]");

		this.defineVariable("op", "Int");
		this.defineVariable("label", "Symbol");
		this.defineVariable("tag", "Symbol");
		this.defineVariable("value", "Array[Byte]");
		this.defineSymbol("len", "%s.length");
		this.defineVariable("data", "Object");

		this.defineVariable("m", "MemoEntry");
		this.defineVariable("key", "Long");
		this.defineVariable("memoPoint", "Int");
		this.defineVariable("result", "Int");

		this.defineSymbol("UtreeLog", "unuseTreeLog");
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
		String block = this.beginBlock();
		block = this.emitStmt(block, String.format("static class %s {", typeName));
		this.incIndent();
		for (String f : fields) {
			f = f.replace("?", "");
			block = this.emitStmt(block, String.format("%s %s;", this.T(f), f));
		}
		/* Constructor */
		block = this.emitStmt(block, String.format("%s(%s) {", typeName.replace("<T>", ""), this.emitParams(fields)));
		this.incIndent();
		this.emitInits(block, fields);
		this.decIndent();
		this.defineSymbol(typeName, "new " + typeName.replace("<T>", "<>"));
		block = this.emitStmt(block, "}");
		this.decIndent();
		block = this.emitStmt(block, "}");
		this.writeSection(this.endBlock(block));
	}

	@Override
	protected void declFuncType(String ret, String typeName, String... params) {
		String block = this.beginBlock();
		block = this.emitStmt(block, String.format("public interface %s {", typeName));
		this.incIndent();
		block = this.emitStmt(block, String.format("%s apply(%s);", ret, this.emitParams(params)));
		this.decIndent();
		block = this.emitStmt(block, "}");
		this.writeSection(this.endBlock(block));
	}

	@Override
	protected String emitChar(int uchar) {
		return "(byte)" + (byte) uchar;
	}

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
		boolean overflow = false;
		int last = 0;
		for (int i = 0; i < 256; i++) {
			if (indexMap[i] != 0) {
				last = i;
			}
			if (indexMap[i] > 35) {
				overflow = true;
			}
		}
		StringBuilder sb = new StringBuilder();
		if (overflow) {
			sb.append("{");
			for (byte index : indexMap) {
				sb.append(index & 0xff);
				sb.append(", ");
			}
			sb.append("}");
		} else {
			sb.append("I(\"");
			for (int i = 0; i <= last; i++) {
				if (indexMap[i] > 10) {
					sb.append((char) ('A' + (indexMap[i] - 10)));
				} else {
					sb.append((char) ('0' + indexMap[i]));
				}
			}
			sb.append("\")");
		}
		return this.getConstName(this.T("indexMap"), sb.toString());
	}

	@Override
	protected String vByteSet(ByteSet bs) {
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
		return this.getConstName(this.T("byteSet"), sb.toString());
	}

	// @Override
	// protected String matchBytes(byte[] text) {
	// return String.format("px.matchBytes(%s)", this.toMultiBytes(text));
	// }
	//
	// protected String toMultiBytes(byte[] text) {
	// StringBuilder sb = new StringBuilder();
	// sb.append("t(\"");
	// for (int i = 0; i < text.length; i++) {
	// int ch = text[i];
	// if (ch >= 0x20 && ch < 0x7e && ch != '\\' && ch != '"') {
	// sb.append((char) ch);
	// } else {
	// sb.append(String.format("~%02x", ch & 0xff));
	// }
	// }
	// sb.append("\")");
	// return this.getConstName("byte[]", sb.toString());
	// }

}
