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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import blue.origami.nez.ast.SourcePosition;
import blue.origami.nez.ast.Symbol;
import blue.origami.nez.parser.Parser;
import blue.origami.nez.parser.ParserGrammar;
import blue.origami.nez.parser.ParserOption;
import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.expression.ByteSet;
import blue.origami.util.OConsole;
import blue.origami.util.OOption;
import blue.origami.util.OptionalFactory;

public abstract class ParserGenerator<B, C> extends RuntimeGenerator<B, C>
		implements OptionalFactory<ParserGenerator<B, C>> {

	protected abstract void initSymbols();

	protected abstract void writeHeader() throws IOException;

	protected abstract void writeFooter() throws IOException;

	protected abstract void declStruct(String typeName, String... fields);

	protected abstract void declFuncType(String ret, String typeName, String... params);

	@Override
	protected abstract void declConst(String typeName, String constName, int arraySize, String literal);

	protected abstract void declProtoType(String ret, String funcName, String[] params);

	protected abstract void declFunc(String ret, String funcName, String[] params, Block<C> block);

	protected void declSymbolTables() {
	}

	// protected abstract void emitFuncReturn(C expr);

	protected abstract C emitParams(String... params);

	protected abstract B beginBlock();

	protected abstract void emitStmt(B block, C expr);

	protected abstract C endBlock(B block);

	protected abstract C emitVarDecl(boolean mutable, String name, C expr);

	protected abstract C emitIfStmt(C expr, boolean elseIf, Block<C> stmt);

	protected abstract C emitWhileStmt(C expr, Block<C> stmt);

	protected final void emitVarDecl(B block, boolean mutable, String name, C expr) {
		this.emitStmt(block, this.emitVarDecl(mutable, name, expr));
	}

	protected final void emitIfStmt(B block, C expr, boolean elseIf, Block<C> stmt) {
		this.emitStmt(block, this.emitIfStmt(expr, elseIf, stmt));
	}

	protected final void emitWhileStmt(B block, C expr, Block<C> stmt) {
		this.emitStmt(block, this.emitWhileStmt(expr, stmt));
	}

	protected final void Assign(B block, String name, C expr) {
		this.emitStmt(block, this.emitAssign(name, expr));
	}

	protected final void Assign2(B block, C left, C expr) {
		this.emitStmt(block, this.emitAssign2(left, expr));
	}

	protected final void Return(B block, C expr) {
		this.emitStmt(block, this.emitReturn(expr));
	}

	protected final void Setter(B block, String name, C expr) {
		this.emitStmt(block, this.emitSetter(name, expr));
	}

	protected abstract C emitAssign(String name, C expr);

	protected abstract C emitAssign2(C left, C expr);

	protected abstract C emitReturn(C expr);

	protected abstract C emitOp(C expr, String op, C expr2);

	protected abstract C emitCast(String var, C expr);

	protected abstract C emitNull();

	protected abstract C emitArrayIndex(C a, C index);

	protected abstract C emitNewArray(String type, C index);

	protected abstract C emitArrayLength(C a);

	protected abstract C emitChar(int uchar);

	protected abstract C emitGetter(C self, String name);

	protected abstract C emitSetter(C self, String name, C expr);

	protected abstract C emitFunc(String func, List<C> params);

	protected abstract C emitApply(C func, List<C> params);

	protected abstract C emitNot(C pe);

	protected abstract C emitSucc();

	protected abstract C emitFail();

	protected abstract C emitAnd(C pe, C pe2);

	protected abstract C emitOr(C pe, C pe2);

	protected abstract C emitIf(C pe, C pe2, C pe3);

	protected abstract C emitDispatch(C index, List<C> cases);

	protected abstract C emitAsm(String expr);

	protected abstract C vInt(int shift);

	protected abstract C vIndexMap(byte[] indexMap);

	protected abstract C vString(String s);

	protected abstract C vValue(String s);

	protected abstract C vTag(Symbol s);

	protected abstract C vLabel(Symbol s);

	protected abstract C vThunk(Object s);

	protected abstract C vByteSet(ByteSet bs);

	protected boolean isFunctional() {
		return false;
	}

	protected final boolean usePointerPosition() {
		return Objects.equals(this.T("pos"), this.T("inputs"));
	}

	protected boolean useDynamicTyping() {
		return this.T("pos") == null;
	}

	protected final boolean useLambda() {
		return this.isDefined("lambda");
	}

	protected final boolean useFuncMap() {
		return this.isDefined("TfuncMap");
	}

	protected final boolean useMultiBytes() {
		return this.isDefined("matchBytes");
	}

	protected abstract C emitFuncRef(String funcName);

	protected abstract C emitParserLambda(C match);

	/* utils */

	public abstract C V(String name);

	public abstract C Const(String name);

	protected final C emitGetter(String name) {
		int loc = name.indexOf('.');
		if (loc == -1) {
			return this.emitGetter(this.V("px"), name);
		} else {
			String self = name.substring(0, loc);
			name = name.substring(loc + 1);
			return this.emitGetter(this.V(self), name);
		}
	}

	protected final C emitSetter(String name, C expr) {
		int loc = name.indexOf('.');
		if (loc == -1) {
			return this.emitSetter(this.V("px"), name, expr);
		} else {
			String self = name.substring(0, loc);
			name = name.substring(loc + 1);
			return this.emitSetter(this.V(self), name, expr);
		}
	}

	protected final C emitBack(String name, C expr) {
		String uFunc = "back" + name;
		if (this.isDefined(uFunc)) {
			expr = this.emitFunc(this.s(uFunc), this.V("px"), expr);
		}
		return this.emitSetter(this.V("px"), name, expr);
	}

	protected void declFunc(String ret, String funcName, Block<C> block) {
		this.declFunc(ret, funcName, new String[0], block);
	}

	protected void declFunc(String ret, String funcName, String a0, Block<C> block) {
		this.declFunc(ret, funcName, new String[] { a0 }, block);
	}

	protected void declFunc(String ret, String funcName, String a0, String a1, Block<C> block) {
		this.declFunc(ret, funcName, new String[] { a0, a1 }, block);
	}

	protected void declFunc(String ret, String funcName, String a0, String a1, String a2, Block<C> block) {
		this.declFunc(ret, funcName, new String[] { a0, a1, a2 }, block);
	}

	protected void declFunc(String ret, String funcName, String a0, String a1, String a2, String a3, Block<C> block) {
		this.declFunc(ret, funcName, new String[] { a0, a1, a2, a3 }, block);
	}

	protected final C emitFunc(String func) {
		List<C> l = new ArrayList<>();
		return this.emitFunc(func, l);
	}

	protected final C emitFunc(String func, C a0) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		return this.emitFunc(func, l);
	}

	protected final C emitFunc(String func, C a0, C a1) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		return this.emitFunc(func, l);
	}

	protected final C emitFunc(String func, C a0, C a1, C a2) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		l.add(a2);
		return this.emitFunc(func, l);
	}

	protected final C emitFunc(String func, C a0, C a1, C a2, C a3) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		l.add(a2);
		l.add(a3);
		return this.emitFunc(func, l);
	}

	protected final C emitFunc(String func, C a0, C a1, C a2, C a3, C a4) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		l.add(a2);
		l.add(a3);
		l.add(a4);
		return this.emitFunc(func, l);
	}

	protected C emitFunc(String func, C a0, C a1, C a2, C a3, C a4, C a5) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		l.add(a2);
		l.add(a3);
		l.add(a4);
		l.add(a5);
		return this.emitFunc(func, l);
	}

	protected C emitFunc(String func, C a0, C a1, C a2, C a3, C a4, C a5, C a6) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		l.add(a2);
		l.add(a3);
		l.add(a4);
		l.add(a5);
		l.add(a6);
		return this.emitFunc(func, l);
	}

	protected final C emitApply(C func, C a0) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		return this.emitApply(func, l);
	}

	protected final C emitApply(C func, C a0, C a1) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		return this.emitApply(func, l);
	}

	protected final C emitApply(C func, C a0, C a1, C a2) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		l.add(a2);
		return this.emitApply(func, l);
	}

	protected final C emitApply(C func, C a0, C a1, C a2, C a3) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		l.add(a2);
		l.add(a3);
		return this.emitApply(func, l);
	}

	protected final C emitApply(C func, C a0, C a1, C a2, C a3, C a4) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		l.add(a2);
		l.add(a3);
		l.add(a4);
		return this.emitApply(func, l);
	}

	protected C emitApply(C func, C a0, C a1, C a2, C a3, C a4, C a5) {
		List<C> l = new ArrayList<>();
		l.add(a0);
		l.add(a1);
		l.add(a2);
		l.add(a3);
		l.add(a4);
		l.add(a5);
		return this.emitApply(func, l);
	}

	// protected C emitEqTag(C expr, C expr2) {
	// return this.emitOp(expr, "==", expr2);
	// }

	protected C emitIsNull(C expr) {
		return this.emitOp(expr, "==", this.emitNull());
	}

	protected C emitIsNotNull(C expr) {
		return this.emitOp(expr, "!=", this.emitNull());
	}

	protected C IfNull(C v, C v2) {
		return this.emitIf(this.emitIsNull(v), v2, v);
	}

	protected C emitNonTerminal(String func) {
		return this.emitFunc(func, this.V("px"));
	}

	protected C emitMove(C shift) {
		this.makeLib("move");
		return this.emitFunc("move", this.V("px"), shift);
	}

	protected C emitMatchAny() {
		this.makeLib("neof");
		return this.emitAnd(this.emitFunc("neof", this.V("px")), this.emitMove(this.vInt(1)));
	}

	protected C emitMatchByte(int uchar) {
		C expr = this.emitFunc("match", this.V("px"), this.emitChar(uchar));
		this.makeLib("match");
		if (uchar == 0) {
			this.makeLib("neof");
			expr = this.emitAnd(expr, this.emitFunc("neof", this.V("px")));
		}
		return expr;
	}

	protected C emitMatchByteSet(ByteSet bs) {
		if (this.isDefined("bitis")) {
			this.makeLib("bitis");
		}
		return this.emitMatchByteSet(bs, null, true);
	}

	protected C emitMatchByteSet(ByteSet bs, C param, boolean proceed) {
		C expr;
		int uchar = bs.getUnsignedByte();
		if (uchar != -1) {
			if (param == null) {
				param = this.emitChar(uchar);
			}
			if (proceed) {
				expr = this.emitFunc("match", this.V("px"), param);
			} else {
				expr = this.emitOp(this.emitFunc("getbyte", this.V("px")), "==", param);
			}
		} else {
			if (param == null) {
				param = this.vByteSet(bs);
			}
			expr = this.emitFunc(proceed ? "nextbyte" : "getbyte", this.V("px"));
			if (this.isDefined("bitis")) {
				// this.makeLib("bitis");
				expr = this.emitFunc("bitis", param, expr);
			} else {
				expr = this.emitArrayIndex(param, expr);
			}
		}
		if (bs.is(0)) {
			if (proceed) {
				expr = this.emitAnd(expr, this.emitFunc("neof", this.V("px")));
			} else {
				expr = this.emitAnd(this.emitFunc("neof", this.V("px")), expr);
			}
		}
		return expr;
	}

	protected abstract C emitUnsigned(C expr);

	protected C emitJumpIndex(byte[] indexMap, boolean proceed) {
		this.makeLib(proceed ? "nextbyte" : "getbyte");
		C a = this.vIndexMap(indexMap);
		C index = this.emitFunc(proceed ? "nextbyte" : "getbyte", this.V("px"));
		boolean hasMinusIndex = false;
		for (byte b : indexMap) {
			if (b < 0) {
				hasMinusIndex = true;
				break;
			}
		}
		if (hasMinusIndex) {
			return this.emitUnsigned(this.emitArrayIndex(a, index));
		}
		return this.emitArrayIndex(a, index);
	}

	protected C emitCheckNonEmpty() {
		return this.emitOp(this.emitGetter("px.pos"), ">", this.V("pos"));
	}

	protected C checkCountVar() {
		return this.emitOp(this.V("cnt"), ">", this.vInt(0));
	}

	protected void initCountVar(B block) {
		this.emitVarDecl(block, true, "cnt", this.vInt(0));
	}

	protected C updateCountVar() {
		return this.emitAssign("cnt", this.emitOp(this.V("cnt"), "+", this.vInt(1)));
	}

	protected C tagTree(Symbol tag) {
		if (this.isTreeConstruction()) {
			this.makeLib("tagT");
			return this.emitFunc("tagT", this.V("px"), this.vTag(tag));
		}
		return this.emitSucc();
	}

	protected C valueTree(String value) {
		if (this.isTreeConstruction()) {
			this.makeLib("valueT");
			return this.emitFunc("valueT", this.V("px"), this.vValue(value));
		}
		return this.emitSucc();
	}

	protected C linkTree(Symbol label) {
		if (this.isTreeConstruction()) {
			this.makeLib("linkT");
			return this.emitFunc("linkT", this.V("px"), this.vTag(label));
		}
		return this.emitSucc();
	}

	protected C foldTree(int beginShift, Symbol label) {
		if (this.isTreeConstruction()) {
			this.makeLib("foldT");
			return this.emitFunc("foldT", this.V("px"), this.vInt(beginShift), this.vTag(label));
		}
		return this.emitSucc();
	}

	protected C beginTree(int beginShift) {
		if (this.isTreeConstruction()) {
			this.makeLib("beginT");
			return this.emitFunc("beginT", this.V("px"), this.vInt(beginShift));
		}
		return this.emitSucc();
	}

	protected C endTree(int endShift, Symbol tag, String value) {
		if (this.isTreeConstruction()) {
			this.makeLib("endT");
			C endTree = this.emitFunc("endT", this.V("px"), this.vInt(endShift), this.vTag(tag));
			if (value != null) {
				endTree = this.emitAnd(this.valueTree(value), endTree);
			}
			return endTree;
		}
		return this.emitSucc();
	}

	protected C callAction(String action, Symbol label, Object thunk) {
		String f = thunk == null ? this.makeLib(action + "S") : this.makeLib(action + "S", thunk);
		return this.emitFunc(f, this.V("px"), this.emitGetter("px.state"), this.vLabel(label),
				this.emitGetter("px.pos"));
	}

	protected C callActionPOS(String action, Symbol label, Object thunk) {
		String f = thunk == null ? this.makeLib(action + "S") : this.makeLib(action + "S", thunk);
		return this.emitFunc(f, this.V("px"), this.emitGetter("px.state"), this.vLabel(label), this.V("pos"));
	}

	/* Optimiztion */

	protected C backLink(C label) {
		this.makeLib("backLink");
		return this.emitFunc("backLink", this.V("px"), this.V("treeLog"), label,
				/* this.vLabel(label) */ this.V("tree"));
	}

	protected C emitInc(C expr) {
		return null;
	}

	protected C matchBytes(byte[] bytes, boolean proceed) {
		C result = null;
		for (byte ch : bytes) {
			if (result != null) {
				result = this.emitAnd(result, this.emitMatchByte(ch & 0xff));
			} else {
				result = this.emitMatchByte(ch & 0xff);
			}
		}
		return result;
	}

	protected void declTree() {
	}

	protected C emitInputString(C text) {
		return text;
	}

	protected C emitNewToken(C tag, C inputs, C pos, C epos) {
		return this.emitNull();
	}

	protected C emitNewTree(C tag, C nsubs) {
		return this.emitNull();
	}

	protected C emitSetTree(C parent, C n, C label, C child) {
		return this.emitNull();
	}

	/* Utils */

	protected final String[] joins(String s, String[] a) {
		if (a.length == 0) {
			return new String[] { s };
		}
		String[] b = new String[a.length + 1];
		b[0] = s;
		System.arraycopy(a, 0, b, 1, a.length);
		return b;
	}

	/* OptionalFactory */

	@Override
	public final Class<?> keyClass() {
		return JavaParserGenerator.class;
	}

	@Override
	public final ParserGenerator<B, C> clone() {
		return this.newClone();
	}

	private OOption options;

	@Override
	public void init(OOption options) {
		this.options = options;
	}

	protected final boolean isTreeConstruction() {
		return this.options.is(ParserOption.TreeConstruction, true);
	}

	protected final String getFileBaseName() {
		String file = this.options.stringValue(ParserOption.GrammarFile, "parser.opeg");
		return SourcePosition.extractFileBaseName(file);
	}

	public final void generate(Grammar g) throws IOException {
		if (g instanceof ParserGrammar) {
			this.generate1((ParserGrammar) g);
		} else {
			Parser p = this.options == null ? g.newParser() : g.newParser(this.options);
			this.generate1(p.getParserGrammar());
		}
	}

	protected ParserGrammar grammar;

	protected int maxDispatch() {
		return this.grammar.maxDispatch();
	}

	public final void generate1(ParserGrammar g) throws IOException {
		ParserGeneratorVisitor<B, C> pgv = new ParserGeneratorVisitor<>();
		this.grammar = g;
		this.initSymbols();
		this.defineVariable("label", this.s("Symbol"));
		this.defineVariable("tag", this.s("Symbol"));
		this.defineVariable("ntag", this.T("cnt"));
		this.defineVariable("nlabel", this.T("cnt"));
		this.defineVariable("value", this.T("inputs"));
		this.defineVariable("nvalue", this.T("cnt"));
		this.defineVariable("pos", this.T("cnt"));
		this.defineVariable("head_pos", this.T("pos"));
		this.defineVariable("shift", this.T("cnt"));
		this.defineVariable("length", this.T("cnt"));
		this.defineVariable("valuelen", this.T("length"));

		this.defineVariable("memoPoint", this.T("cnt"));
		this.defineVariable("result", this.T("cnt"));
		this.defineVariable("prevLog", this.T("treeLog"));
		this.defineVariable("nextLog", this.T("treeLog"));
		this.defineVariable("prevState", this.T("state"));
		this.defineVariable("nextState", this.T("state"));
		this.defineVariable("uLog", this.T("treeLog"));
		this.defineVariable("uState", this.T("state"));
		this.defineVariable("epos", this.T("pos"));
		this.defineVariable("child", this.T("tree"));
		this.defineVariable("f2", this.T("f"));
		this.defineSymbol("Intag", "0");
		this.defineSymbol("Ikey", "-1");
		this.defineSymbol("Icnt", "0");
		this.defineSymbol("Iop", "0");
		this.defineSymbol("Ipos", "0");
		this.defineSymbol("Ihead_pos", "0");
		this.defineSymbol("Iresult", "0");

		this.defineVariable("indexMap", "array");
		this.defineVariable("byteSet", "array");
		this.defineVariable("s", this.T("byteSet"));
		// this.defineVariable("funcMap", "array");

		this.defineSymbol("{[", "{");
		this.defineSymbol("]}", "}");

		this.loadContext(this, g);
		this.loadTreeLog(this);
		this.loadState(this);
		this.loadMemo(this, g);
		pgv.loadCombinator(this);
		this.loadMain(this);
		this.makeLib("NezParserContext");
		pgv.start(g, this);
		ArrayList<String> funcList = this.sortFuncList("start");
		for (String funcName : this.crossRefNames) {
			this.declProtoType(this.T("matched"), funcName, new String[] { "px" });
		}
		this.writeHeader();
		this.writeComment("*** const ***");
		this.writeLine(this.head.toString());
		this.writeComment("*** libs ***");
		this.writeLine(this.lib.toString());

		for (String funcName : funcList) {
			SourceSection s = this.sectionMap.get(funcName);
			if (s != null) {
				this.write(s);
			}
		}
		this.lib = new SourceSection();
		this.writeFooter();
	}

	protected void log(String line, Object... args) {
		OConsole.println(line, args);
	}

	private boolean isBinary;
	private boolean isStateful;

	public final void initGrammarProperty(boolean binary, boolean isStateful) {
		this.isBinary = binary;
		this.isStateful = isStateful;
	}

	protected boolean isBinary() {
		return this.isBinary;
	}

	protected boolean isStateful() {
		return this.isStateful;
	}

	protected String localName(String funcName) {
		return funcName;
	}

}
