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

import blue.nez.ast.SourcePosition;
import blue.nez.ast.Symbol;
import blue.nez.parser.Parser;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.parser.ParserContext.SymbolPredicate;
import blue.nez.parser.ParserGrammar;
import blue.nez.parser.ParserOption;
import blue.nez.peg.Expression;
import blue.nez.peg.Grammar;
import blue.nez.peg.expression.ByteSet;
import blue.origami.util.OConsole;
import blue.origami.util.OOption;
import blue.origami.util.OptionalFactory;

public abstract class AbstractParserGenerator<C> extends RuntimeGenerator<C>
		implements OptionalFactory<AbstractParserGenerator<C>> {

	protected abstract void initSymbols();

	protected abstract void writeHeader() throws IOException;

	protected abstract void writeFooter() throws IOException;

	protected abstract void declStruct(String typeName, String... fields);

	protected abstract void declFuncType(String ret, String typeName, String... params);

	@Override
	protected abstract void declConst(String typeName, String constName, String literal);

	protected abstract void declProtoType(String funcName);

	protected abstract void declFunc(String ret, String funcName, String[] params, Block<C> block);

	// protected abstract void emitFuncReturn(C expr);

	protected abstract C emitParams(String... params);

	protected abstract C beginBlock();

	protected abstract C emitStmt(C block, C expr);

	protected abstract C endBlock(C block);

	protected abstract C emitReturn(C expr);

	protected abstract C emitVarDecl(String name, C expr);

	protected abstract C emitAssign(String name, C expr);

	protected abstract C emitAssign2(C left, C expr);

	protected abstract C emitIfStmt(C expr, Block<C> stmt);

	protected abstract C emitWhileStmt(C expr, Block<C> stmt);

	protected abstract C emitOp(C expr, String op, C expr2);

	protected abstract C emitCast(String var, C expr);

	protected abstract C emitNull();

	protected abstract C emitArrayIndex(C a, C index);

	protected abstract C emitNewArray(String type, C index);

	protected abstract C emitChar(int uchar);

	protected abstract C emitGetter(C self, String name);

	protected abstract C emitSetter(C self, String name, C expr);

	protected abstract C emitFunc(String func, List<C> params);

	protected abstract C emitApply(C func);

	protected abstract C emitNot(C pe);

	protected abstract C emitSucc();

	protected abstract C emitFail();

	protected abstract C emitAnd(C pe, C pe2);

	protected abstract C emitOr(C pe, C pe2);

	protected abstract C emitIf(C pe, C pe2, C pe3);

	// protected abstract C emitLoopElse(C pe, Block<C> stmt, C pe3);

	protected abstract C emitDispatch(C index, List<C> cases);

	protected abstract C emitAsm(String expr);

	// protected abstract void beginDefine(String funcName, Expression e);
	//
	// protected abstract void endDefine(String funcName, C pe);
	//
	// protected abstract C returnResult(C pe);

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

	protected boolean supportedLambdaFunction() {
		return false;
	}

	protected abstract C emitFuncRef(String funcName);

	protected abstract C emitParserLambda(C match);

	/* utils */

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
		String uFunc = "U" + name;
		if (this.isDefinedSymbol(uFunc)) {
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

	protected C emitNonTerminal(String func) {
		return this.emitFunc(func, this.V("px"));
	}

	protected C emitMove(int shift) {
		return this.emitFunc("move", this.V("px"), this.vInt(shift));
	}

	protected C emitMatchAny() {
		return this.emitNot(this.emitMatchByte(0));
	}

	protected C emitMatchEOF() {
		return this.emitFunc("eof", this.V("px"));
	}

	protected C emitMatchByte(int uchar) {
		return this.emitFunc("match", this.V("px"), this.emitChar(uchar));
	}

	protected C emitMatchByteSet(ByteSet bs) {
		int uchar = bs.getUnsignedByte();
		if (uchar != -1) {
			return this.emitMatchByte(uchar);
		} else {
			C nextbyte = this.emitUnsigned(this.emitFunc("nextbyte", this.V("px")));
			if (this.isDefinedSymbol("bitis")) {
				return this.emitFunc("bitis", this.vByteSet(bs), nextbyte);
			} else {
				return this.emitArrayIndex(this.vByteSet(bs), nextbyte);
			}
		}
	}

	protected C emitUnsigned(C expr) {
		return this.emitOp(expr, "&", this.vInt(0xff));
	}

	protected C emitJumpIndex(byte[] indexMap) {
		C a = this.vIndexMap(indexMap);
		C index = this.emitUnsigned(this.emitFunc("getbyte", this.V("px")));
		return this.emitArrayIndex(a, index);
	}

	protected C emitCheckNonEmpty() {
		return this.emitOp(this.emitGetter("px.pos"), ">", this.V("pos"));
	}

	protected C checkCountVar() {
		return this.emitOp(this.V("cnt"), ">", this.vInt(0));
	}

	protected C initCountVar() {
		return this.emitVarDecl("cnt", this.vInt(0));
	}

	protected C updateCountVar() {
		return this.emitAssign("cnt", this.emitOp(this.V("cnt"), "+", this.vInt(1)));
	}

	protected C tagTree(Symbol tag) {
		return this.emitFunc("tagTree", this.V("px"), this.vTag(tag));
	}

	protected C valueTree(String value) {
		return this.emitFunc("valueTree", this.V("px"), this.vValue(value));
	}

	protected C linkTree(Symbol label) {
		return this.emitFunc("linkTree", this.V("px"), this.vTag(label));
	}

	protected C foldTree(int beginShift, Symbol label) {
		return this.emitFunc("foldTree", this.V("px"), this.vInt(beginShift), this.vTag(label));
	}

	protected C beginTree(int beginShift) {
		return this.emitFunc("beginTree", this.V("px"), this.vInt(beginShift));
	}

	protected C endTree(int endShift, Symbol tag, String value) {
		return this.emitFunc("endTree", this.V("px"), this.vInt(endShift), this.vTag(tag), this.vValue(value));
	}

	// protected C memoLookup(int memoId, boolean withTree) {
	// return this.emitFunc("lookupMemo" + (withTree ? "3" : "1"), this.V("px"),
	// this.vInt(memoId));
	// }
	//
	// protected C memoSucc(C memoId) {
	// return this.emitFunc("storeMemo", this.V("px"), memoId, this.V("pos"),
	// this.vInt(ResultSucc));
	// }
	//
	// protected C memoFail(C memoId) {
	// return this.emitFunc("storeMemo", this.V("px"), memoId, this.V("pos"),
	// this.vInt(ResultFail));
	// }

	protected C callAction(SymbolAction action, Symbol label, Object thunk) {
		return this.emitFunc("callAction", this.V("px"), this.emitFuncRef(action.toString()), this.vLabel(label),
				this.vInt(-1), this.vThunk(thunk));
	}

	protected C callAction(SymbolAction action, Symbol label, int suffix, Object thunk) {
		return this.emitFunc("callAction", this.V("px"), this.emitFuncRef(action.toString()), this.vLabel(label),
				this.V("pos"), this.vThunk(thunk));
	}

	protected C callPredicate(SymbolPredicate pred, Symbol label, Object thunk) {
		return this.emitFunc("callPredicate", this.V("px"), this.emitFuncRef(pred.toString()), this.vLabel(label),
				this.vInt(-1), this.vThunk(thunk));
	}

	protected C callPredicate(SymbolPredicate pred, Symbol label, int suffix, Object thunk) {
		return this.emitFunc("callPredicate", this.V("px"), this.emitFuncRef(pred.toString()), this.vLabel(label),
				this.V("pos"), this.vThunk(thunk));
	}

	// protected C vValue(String s) {
	// if (s != null) {
	// StringBuilder sb = new StringBuilder();
	// OStringUtils.formatStringLiteral(sb, '"', s, '"');
	// return sb.toString();
	// }
	// return this.s("null");
	// }
	//
	// protected C vTag(Symbol s) {
	// return this.vLabel(s);
	// }
	//
	// protected C vLabel(Symbol s) {
	// if (s != null) {
	// StringBuilder sb = new StringBuilder();
	// OStringUtils.formatStringLiteral(sb, '"', s.toString(), '"');
	// return sb.toString();
	// }
	// return this.s("null");
	// }
	//
	// protected C vThunk(Object thunk) {
	// return this.s("null");
	// }
	//
	// protected C toValue(String typeName, byte[] indexMap) {
	// StringBuilder sb = new StringBuilder();
	// sb.append("{");
	// for (byte index : indexMap) {
	// sb.append(index & 0xff);
	// sb.append(", ");
	// }
	// sb.append("}");
	// return sb.toString();
	// }
	//
	// protected C toValue(String typeName, ByteSet bs) {
	// StringBuilder sb = new StringBuilder();
	// sb.append("{");
	// for (int index : bs.bits()) {
	// sb.append(index);
	// sb.append(", ");
	// }
	// sb.append("}");
	// return this.getConstName(typeName, sb.toString());
	// }

	/* Optimiztion */

	protected C backLink(Symbol label) {
		return this.emitFunc("backLink", this.V("px"), this.V("treeLog"), this.vLabel(label), this.V("tree"));
	}

	protected boolean useMultiBytes() {
		return false;
	}

	protected C emitInc(C expr) {
		return null;
	}

	protected C matchBytes(byte[] bytes) {
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

	protected C matchMany(Expression e) {
		return null;
	}

	protected C matchOption(Expression e) {
		return null;
	}

	protected C matchAnd(Expression e) {
		return null;
	}

	protected C matchNot(Expression e) {
		return null;
	}

	@Override
	public final Class<?> keyClass() {
		return JavaParserGenerator.class;
	}

	@Override
	public final AbstractParserGenerator<C> clone() {
		return this.newClone();
	}

	private OOption options;

	@Override
	public void init(OOption options) {
		this.options = options;
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

	public final void generate1(ParserGrammar g) throws IOException {
		ParserGeneratorVisitor<C> pgv = new ParserGeneratorVisitor<>();
		this.initSymbols();
		this.makeMemoLibs(this, g.getMemoPointSize(), 64);
		this.makeTreeLibs(this);
		this.makeMatchLibs(this);
		pgv.start(g, this);
		ArrayList<String> funcList = this.sortFuncList("start");
		for (String funcName : this.crossRefNames) {
			this.declProtoType(funcName);
		}
		this.writeHeader();
		this.writeLine(this.lib.toString());
		this.writeLine(this.head.toString());
		for (String funcName : funcList) {
			SourceSection s = this.sectionMap.get(funcName);
			if (s != null) {
				this.write(s);
			}
		}
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
