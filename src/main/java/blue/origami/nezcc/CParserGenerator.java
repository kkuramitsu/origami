/// ***********************************************************************
// * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// ***********************************************************************/
//
// package blue.origami.nezcc;
//
// import java.io.IOException;
// import java.util.List;
//
// import blue.nez.ast.Symbol;
// import blue.nez.peg.expression.ByteSet;
// import blue.origami.util.OStringUtils;
//
// public class CParserGenerator extends ParserGenerator {
// @Override
// protected void initSymbols() {
// this.defineSymbol("\t", " ");
//
// this.defineSymbol("Tmatched", "int");
// this.defineSymbol("bool", "int");
// this.defineSymbol("Tpx", "struct NezParserContext*");
// this.defineSymbol("Tinputs", "const char*");
// this.defineSymbol("Tpos", "size_t");
// this.defineSymbol("Ipos", "0");
// this.defineSymbol("TtreeLog", "struct TreeLog*");
// this.defineSymbol("Ttree", "void*");
// this.defineSymbol("Tstate", "void*");
// this.defineSymbol("Tmemos", "struct MemoEntry*");
//
// this.defineSymbol("TnewFunc", "TreeFunc");
// this.defineSymbol("TsetFunc", "TreeSetFunc");
// this.defineSymbol("Tf", "ParserFunc");
// this.defineSymbol("Tf2", "ParserFunc");
//
// this.defineSymbol("px.newTree", "px->newFunc");
// this.defineSymbol("px.setTree", "px->setFunc");
// // this.defineSymbol("Mf", "match");
// // this.defineSymbol("Mf2", "match");
//
// this.defineSymbol("Tch", "char");
// this.defineSymbol("Tcnt", "int");
// this.defineSymbol("Tshift", "int");
// this.defineSymbol("TindexMap", "unsigned char[256]");
// this.defineSymbol("TbyteSet", "int[8]");
//
// this.defineSymbol("Top", "int");
// this.defineSymbol("Tlabel", "const char*");
// this.defineSymbol("Ttag", "const char*");
// this.defineSymbol("Tvalue", "const char*");
// this.defineSymbol("Tdata", "void*");
// this.defineSymbol("TprevLog", "TreeLog");
//
// this.defineSymbol("Tm", "struct MemoEntry*");
// this.defineSymbol("Tkey", "unsigned long long");
// this.defineSymbol("TmemoPoint", "int");
// this.defineSymbol("Tresult", "int");
// }
//
// @Override
// protected void writeHeader() throws IOException {
// this.open(this.getFileBaseName() + ".c");
// // this.writeResource("/blue/origami/nezcc/javaparser-imports.java");
// // this.writeLine("");
// // this.writeLine("class %s {", this.getFileBaseName());
// // this.writeResource("/blue/origami/nezcc/javaparser-libs.java");
// }
//
// @Override
// protected void writeFooter() throws IOException {
// // this.writeResource("/blue/origami/nezcc/javaparser-main.java");
// // this.writeLine("}");
// // this.showResource("/blue/origami/nezcc/javaparser-man.txt", "$cmd$",
// // this.getFileBaseName());
// }
//
// @Override
// protected void declConst(String typeName, String constName, String literal) {
// int loc = typeName.indexOf("[");
// if (loc > 0) {
// typeName = typeName.substring(0, loc) + "[]";
// }
// this.writeSection(String.format("static %s %s = %s;", typeName, constName,
/// literal));
// }
//
// @Override
// protected void definePrototype(String funcName) {
// }
//
// @Override
// protected String emitSucc() {
// return this.s("1");
// }
//
// @Override
// protected String emitFail() {
// return this.s("0");
// }
//
// @Override
// protected String emitChar(int uchar) {
// return "(char)" + (byte) uchar;
// }
//
// @Override
// protected String vByteSet(ByteSet bs) {
// int last = 0;
// for (int i = 0; i < 256; i++) {
// if (bs.is(i)) {
// last = i;
// }
// }
// StringBuilder sb = new StringBuilder();
// sb.append("boolMap(\"");
// for (int i = 0; i <= last; i++) {
// sb.append(bs.is(i) ? "1" : "0");
// }
// sb.append("\")");
// return this.getConstName(this.T("byteSet"), sb.toString());
// }
//
// /* dispatch */
//
// @Override
// protected String vValue(String s) {
// if (s != null) {
// StringBuilder sb = new StringBuilder();
// OStringUtils.formatStringLiteral(sb, '"', s, '"');
// return sb.toString();
// }
// return this.s("NULL");
// }
//
// @Override
// protected String vLabel(Symbol s) {
// if (s != null) {
// StringBuilder sb = new StringBuilder();
// OStringUtils.formatStringLiteral(sb, '"', s.toString(), '"');
// return sb.toString();
// }
// return this.s("NULL");
// }
//
// @Override
// protected boolean supportedLambdaFunction() {
// return false;
// }
//
// @Override
// protected String emitFuncRef(String funcName) {
// return String.format("%s", funcName);
// }
//
// @Override
// protected String emitParserLambda(String match) {
// return null;
// }
//
// /* Optimization */
//
// @Override
// protected boolean useMultiBytes() {
// return false;
// }
//
// // @Override
// // protected String matchBytes(byte[] text) {
// // return String.format("px.matchBytes(%s)", this.toMultiBytes(text));
// // }
// //
// // protected String toMultiBytes(byte[] text) {
// // StringBuilder sb = new StringBuilder();
// // sb.append("t(\"");
// // for (int i = 0; i < text.length; i++) {
// // int ch = text[i];
// // if (ch >= 0x20 && ch < 0x7e && ch != '\\' && ch != '"') {
// // sb.append((char) ch);
// // } else {
// // sb.append(String.format("~%02x", ch & 0xff));
// // }
// // }
// // sb.append("\")");
// // return this.getConstName("byte[]", sb.toString());
// // }
//
// @Override
// protected void declFunc(String ret, String funcName, String... params) {
// this.writeSection(String.format("static %s %s%s {", ret, funcName,
/// this.emitParams(params)));
// this.incIndent();
// }
//
// @Override
// protected void emitFuncReturn(String expr) {
// this.writeSection(this.returnResult(expr));
// this.decIndent();
// this.writeSection("}");
// }
//
// @Override
// protected String emitParams(String... params) {
// StringBuilder sb = new StringBuilder();
// sb.append("(");
// int c = 0;
// for (String p : params) {
// if (p.endsWith("?")) {
// continue;
// }
// if (c > 0) {
// sb.append(", ");
// }
// c++;
// String t = this.T(p);
// if (t == null) {
// sb.append(p);
// } else {
// sb.append(String.format("%s %s", t, p));
// }
// }
// sb.append(")");
// return sb.toString();
// }
//
// protected String returnResult(String pe) {
// if (pe.startsWith(" ") || pe.startsWith("\t")) {
// return pe;
// }
// if (this.isDebug()) {
// // return this.Indent("return B(\"%s\", px) && E(\"%s\", px, %s);",
// // this.getCurrentFuncName(),
// // this.getCurrentFuncName(), pe);
// }
// return this.Indent(this.emitReturn(pe));
// }
//
// // @Override
// // protected String emitFuncTypeDecl(String ret, String typeName, String...
// // params) {
// // String block = this.beginDo();
// // block = this.emitStmt(block, String.format("static interface %s {",
// // typeName));
// // this.incIndent();
// // for (String f : fields) {
// // block = this.emitStmt(block, String.format("%s %s;", this.T(f), f));
// // }
// // this.decIndent();
// // block = this.emitStmt(block, "}");
// // return this.endDo(block);
// // }
//
// @Override
// protected String emitGetter(String self, String name) {
// return String.format("%s->%s", this.s(self), this.s(name));
// }
//
// @Override
// protected String emitSetter(String self, String name, String expr) {
// return String.format("%s->%s = %s;", this.s(self), this.s(name), expr);
// }
//
// @Override
// protected String emitReturn(String expr) {
// return String.format("return %s;", expr);
// }
//
// @Override
// protected String emitVarDecl(String name, String expr) {
// return String.format("%s %s = %s;", this.T(name), this.s(name), expr);
// }
//
// @Override
// protected String emitAssign(String name, String expr) {
// return String.format("%s = %s;", this.s(name), expr);
// }
//
// @Override
// protected String emitAssign2(String left, String expr) {
// return String.format("%s = %s;", left, expr);
// }
//
// @Override
// protected String emitApply(String func) {
// return String.format("%s(%s)", func, this.s("px"));
// }
//
// @Override
// protected String beginBlock() {
// return "";
// }
//
// @Override
// protected String emitStmt(String block, String expr) {
// String stmt = this.Indent(expr.trim());
// if (stmt.endsWith(")")) {
// stmt = stmt + ";";
// }
// if (!block.equals("")) {
// stmt = block + "\n" + stmt;
// }
// return stmt;
// }
//
// @Override
// protected String endBlock(String block) {
// return block;
// }
//
// @Override
// protected String emitOp(String expr, String op, String expr2) {
// return String.format("%s %s %s", expr, this.s(op), expr2);
// }
//
// @Override
// protected String emitAsm(String expr) {
// return this.s(expr);
// }
//
// @Override
// protected String emitCast(String var, String expr) {
// return String.format("(%s)(%s)", this.T(var), expr);
// }
//
// @Override
// protected String emitNull() {
// return this.s("NULL");
// }
//
// @Override
// protected String emitNewArray(String type, String index) {
// return String.format("_calloc(sizeof(%s), %s)", type, index);
// }
//
// @Override
// protected String emitArrayIndex(String a, String index) {
// return String.format("%s[%s]", a, index);
// }
//
// @Override
// protected String emitFunc(String func, List<String> params) {
// StringBuilder sb = new StringBuilder();
// sb.append(this.s(func));
// sb.append("(");
// int c = 0;
// for (String p : params) {
// if (c > 0) {
// sb.append(",");
// }
// c++;
// sb.append(p);
// }
// sb.append(")");
// return sb.toString();
// }
//
// @Override
// protected String emitNot(String expr) {
// return String.format("!(%s)", expr);
// }
//
// @Override
// protected String emitAnd(String expr, String expr2) {
// return String.format("%s && %s", expr, expr2);
// }
//
// @Override
// protected String emitOr(String expr, String expr2) {
// return String.format("(%s) || (%s)", expr, expr2);
// }
//
// @Override
// protected String emitIf(String expr, String expr2, String expr3) {
// return String.format("(%s) ? (%s) : (%s)", expr, expr2, expr3);
// }
//
// @Override
// protected String emitIfStmt(String expr, Block<String> stmt) {
// String block = this.beginBlock();
// block = this.emitStmt(block, String.format("if(%s) {", expr));
// this.incIndent();
// block = this.emitStmt(block, stmt.block());
// this.decIndent();
// block = this.emitStmt(block, "}");
// return this.endBlock(block);
// }
//
// @Override
// protected String emitWhileStmt(String expr, Block<String> stmt) {
// String block = this.beginBlock();
// block = this.emitStmt(block, String.format("while(%s) {", expr));
// this.incIndent();
// block = this.emitStmt(block, stmt.block());
// this.decIndent();
// block = this.emitStmt(block, "}");
// return this.endBlock(block);
// }
//
// @Override
// protected String emitDispatch(String index, List<String> cases) {
// String block = this.beginBlock();
// block = this.emitStmt(block, String.format("switch(%s) {", index));
// this.incIndent();
// for (int i = 1; i < cases.size(); i++) {
// block = this.emitStmt(block, String.format("case %s: return %s;", i,
/// cases.get(i)));
// }
// block = this.emitStmt(block, String.format("default: return %s;",
/// cases.get(0)));
// this.decIndent();
// block = this.emitStmt(block, "}");
// return this.endBlock(block);
// }
//
// @Override
// protected String emitStructDecl(String typeName, String... fields) {
// String block = this.beginBlock();
// block = this.emitStmt(block, String.format("%s {", typeName.replace("*",
/// "")));
// this.incIndent();
// for (String f : fields) {
// f = f.replace("?", "");
// block = this.emitStmt(block, String.format("%s %s;", this.T(f), f));
// }
// // /* Constructor */
// // block = this.emitStmt(block, String.format("%s%s {",
// // typeName.replace("<T>", ""), this.emitParams(fields)));
// // this.incIndent();
// // for (String f : fields) {
// // f = f.replace("?", "");
// // block = this.emitStmt(block, String.format("this.%s = %s;", f, f));
// // }
// // this.decIndent();
// // this.defineSymbol(typeName, "new " + typeName.replace("<T>", "<>"));
// // block = this.emitStmt(block, "}");
//
// this.decIndent();
// block = this.emitStmt(block, "};");
// return this.endBlock(block);
// }
//
// @Override
// protected String vIndexMap(byte[] indexMap) {
// StringBuilder sb = new StringBuilder();
// sb.append("{");
// for (byte index : indexMap) {
// sb.append(index & 0xff);
// sb.append(", ");
// }
// sb.append("}");
// return this.getConstName(this.T("indexMap"), sb.toString());
// }
//
// @Override
// protected String vInt(int value) {
// return "" + value;
// }
//
// @Override
// protected String vTag(Symbol s) {
// return this.vLabel(s);
// }
//
// @Override
// protected String vThunk(Object s) {
// // TODO Auto-generated method stub
// return null;
// }
//
// @Override
// public String V(String name) {
// return this.s(name);
// }
//
// }
