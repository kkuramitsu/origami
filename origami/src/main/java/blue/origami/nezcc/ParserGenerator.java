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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import blue.nez.ast.SourcePosition;
import blue.nez.ast.Symbol;
import blue.nez.parser.Parser;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.parser.ParserContext.SymbolPredicate;
import blue.nez.parser.ParserContext.SymbolReset;
import blue.nez.parser.ParserGrammar;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.ParserOption;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.Grammar;
import blue.nez.peg.NonEmpty;
import blue.nez.peg.Production;
import blue.nez.peg.Stateful;
import blue.nez.peg.Typestate;
import blue.nez.peg.expression.ByteSet;
import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDetree;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PIfCondition;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POnCondition;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PRepeat;
import blue.nez.peg.expression.PRepetition;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.nez.peg.expression.PValue;
import blue.origami.util.OCommonWriter;
import blue.origami.util.OConsole;
import blue.origami.util.OOption;
import blue.origami.util.OStringUtils;
import blue.origami.util.OptionalFactory;

public abstract class ParserGenerator extends CodeBase implements OptionalFactory<ParserGenerator> {

	@Override
	public final Class<?> keyClass() {
		return JavaParserGenerator.class;
	}

	@Override
	public final ParserGenerator clone() {
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
		ParserGeneratorVisitor pgv = new ParserGeneratorVisitor();
		if (g instanceof ParserGrammar) {
			pgv.start((ParserGrammar) g, this);
		} else {
			Parser p = this.options == null ? g.newParser() : g.newParser(this.options);
			pgv.start(p.getParserGrammar(), this);
		}
		ArrayList<String> funcList = this.sortFuncList("start");
		for (String funcName : this.crossRefNames) {
			this.definePrototype(funcName);
		}
		this.writeHeader();
		this.out.println(this.head.toString());
		for (String funcName : funcList) {
			SourceFragment s = this.codeMap.get(funcName);
			if (s != null) {
				// if (!this.crossRefNames.contains(funcName)) {
				// this.out.print("static inline");
				// }
				this.out.p(s);
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

	protected abstract void writeHeader() throws IOException;

	protected abstract void writeFooter() throws IOException;

	@Override
	protected abstract void defineConst(String typeName, String constName, String literal);

	protected abstract void definePrototype(String funcName);

	protected abstract void beginDefine(String funcName, Expression e);

	protected abstract void endDefine(String funcName, String pe);

	protected abstract String result(String pe);

	protected abstract String move(int shift);

	protected abstract String matchSucc();

	protected abstract String matchFail();

	protected abstract String matchAny();

	protected abstract String matchByte(int uchar);

	protected abstract String matchByteSet(ByteSet byteSet);

	protected abstract String matchNonTerminal(String func);

	protected abstract String matchPair(String pe, String pe2);

	protected abstract String matchChoice(String pe, String pe2);

	protected abstract String matchIf(String pe, String pe2, String pe3);

	protected abstract String matchLoop(String pe, String pe2, String pe3);

	protected abstract String checkCountVar(int varid);

	protected abstract String matchCase(String jumpIndex, String[] exprs);

	protected abstract String fetchJumpIndex(byte[] indexMap);

	protected abstract String checkNonEmpty(int varid);

	protected abstract String initPosVar(int varid);

	protected abstract String initTreeVar(int varid);

	protected abstract String initLogVar(int varid);

	protected abstract String initStateVar(int varid);

	protected abstract String backPos(int varid);

	protected abstract String backTree(int varid);

	protected abstract String backTreeLog(int varid);

	protected abstract String backState(int varid);

	protected abstract String updatePos(int varid);

	protected abstract String updateTree(int varid);

	protected abstract String updateTreeLog(int varid);

	protected abstract String updateState(int varid);

	protected abstract String initCountVar(int varid);

	protected abstract String updateCountVar(int varid);

	protected abstract String tagTree(Symbol tag);

	protected abstract String valueTree(String value);

	protected abstract String linkTree(int varid, Symbol label);

	protected abstract String foldTree(int beginShift, Symbol label);

	protected abstract String beginTree(int beginShift);

	protected abstract String endTree(int endShift, Symbol tag, String value);

	protected abstract String memoDispatch(String lookup, String main);

	protected abstract String memoLookup(int memoId, boolean withTree);

	protected abstract String memoSucc(int varid, int memoId, boolean withTree);

	protected abstract String memoFail(int varid, int memoId);

	protected abstract String callAction(SymbolAction action, Symbol label, Object thunk);

	protected abstract String callAction(SymbolAction action, Symbol label, int varid, Object thunk);

	protected abstract String callPredicate(SymbolPredicate pred, Symbol label, Object thunk);

	protected abstract String callPredicate(SymbolPredicate pred, Symbol label, int varid, Object thunk);

	protected String toLiteral(String s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatStringLiteral(sb, '"', s, '"');
			return sb.toString();
		}
		return this.s("null");
	}

	protected String toLiteral(Symbol s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatStringLiteral(sb, '"', s.toString(), '"');
			return sb.toString();
		}
		return this.s("null");
	}

	protected String toLiteral(byte[] indexMap) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (byte index : indexMap) {
			sb.append(index & 0xff);
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

	protected String toLiteral(ByteSet bs) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (int index : bs.bits()) {
			sb.append(index);
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

	/* Optimiztion */

	protected String back2(int varid) {
		return this.matchPair(this.backPos(varid), this.backState(varid));
	}

	protected String back3(int varid) {
		String pe = this.matchPair(this.backTree(varid), this.backTreeLog(varid));
		return this.matchPair(this.backPos(varid), pe);
	}

	protected String back4(int varid) {
		return this.matchPair(this.back3(varid), this.backState(varid));
	}

	protected String backLink(int varid, Symbol label) {
		String pe = this.matchPair(this.linkTree(varid, label), this.backTree(varid));
		return this.matchPair(this.backTreeLog(varid), pe);
	}

	protected boolean useMultiBytes() {
		return false;
	}

	protected String matchBytes(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		int c = 0;
		for (byte ch : bytes) {
			if (c > 0) {
				sb.append(this.s(" "));
				sb.append(this.s("&&"));
				sb.append(this.s(" "));
			}
			sb.append(this.matchByte(ch & 0xff));
			c++;
		}
		return sb.toString();
	}

	protected boolean useLexicalOptimization() {
		return false;
	}

	protected String matchRepetition(ByteSet byteSet) {
		return null;
	}

	protected String matchAnd(ByteSet byteSet) {
		return null;
	}

	protected String matchNot(ByteSet byteSet) {
		return null;
	}

	protected String matchOption(ByteSet byteSet) {
		return null;
	}

	protected boolean supportedLambdaFunction() {
		return false;
	}

	protected String refFunc(String funcName) {
		return null;
	}

	protected String defineLambda(String match) {
		return null;
	}

	protected String getRepetitionCombinator() {
		return null;
	}

	protected String getOptionCombinator() {
		return null;
	}

	protected String getAndCombinator() {
		return null;
	}

	protected String getNotCombinator() {
		return null;
	}

	protected String getLinkCombinator() {
		return null;
	}

	protected String getMemoCombinator() {
		return null;
	}

	protected String callCombinator(String combi, String funcName) {
		return null;
	}

	protected String callCombinator(String combi, Symbol label, String funcName) {
		return null;
	}

	protected String callCombinator(String combi, int memoPoint, String funcName) {
		return null;
	}

}

class SourceFragment {
	StringBuilder sb = new StringBuilder();
	int indent = 0;

	public void incIndent() {
		this.indent++;
	}

	public void decIndent() {
		this.indent--;
	}

	public void L(String fmt, Object... args) {
		this.sb.append("\n");
		for (int i = 0; i < this.indent; i++) {
			this.sb.append("   ");
		}
		if (args.length == 0) {
			this.sb.append(fmt);
		} else {
			this.sb.append(String.format(fmt, args));
		}
	}

	public String Line(String fmt, Object... args) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.indent; i++) {
			sb.append("   ");
		}
		if (args.length == 0) {
			sb.append(fmt);
		} else {
			sb.append(String.format(fmt, args));
		}
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public String toString() {
		return this.sb.toString();
	}
}

abstract class CodeBase {

	private boolean debug = System.getenv("DEBUG") != null;

	protected boolean isDebug() {
		return this.debug;
	}

	protected SourceFragment head = new SourceFragment();
	private SourceFragment body = this.head;

	protected String Line(String fmt, Object... args) {
		return this.body.Line(fmt, args);
	}

	protected String Expr(String fmt, Object... args) {
		return String.format(fmt, args);
	}

	protected void L(String fmt, Object... args) {
		this.body.L(fmt, args);
	}

	protected void Comment(String fmt, Object... args) {
		this.body.L("// " + fmt, args);
	}

	protected void incIndent() {
		this.body.incIndent();
	}

	protected void decIndent() {
		this.body.decIndent();
	}

	protected HashMap<String, SourceFragment> codeMap = new HashMap<>();

	public boolean isDefined(String funcName) {
		return this.codeMap.containsKey(funcName);
	}

	protected void openFragment(String funcName) {
		SourceFragment s = new SourceFragment();
		this.codeMap.put(funcName, s);
		this.body = s;
	}

	protected void closeFlagment() {
		// System.out.println("DEBUG " + this.out);
		this.body = this.head;
	}

	private String currentFuncName = null;

	void setCurrentFuncName(String funcName) {
		this.currentFuncName = funcName;
		this.u = 0;
	}

	String getCurrentFuncName() {
		return this.currentFuncName;
	}

	private int u = 0;

	public final int uniqueVarId() {
		return this.u++;
	}

	public final String v(String name, int varid) {
		return varid == 0 ? name : name + varid;
	}

	HashMap<String, String> symbolMap = new HashMap<>();

	protected void defineSymbol(String key, String symbol) {
		this.symbolMap.put(key, symbol);
	}

	protected String s(String key) {
		return this.symbolMap.getOrDefault(key, key);
	}

	protected String getConstName(String typeName, String typeLiteral) {
		String key = typeName + typeLiteral;
		String constName = this.symbolMap.get(key);
		if (constName == null) {
			constName = "c" + this.symbolMap.size();
			this.symbolMap.put(key, constName);
			SourceFragment body = this.body;
			this.body = this.head;
			this.defineConst(typeName, constName, typeLiteral);
			this.body = body;
		}
		return constName;
	}

	protected abstract void defineConst(String typeName, String constName, String literal);

	// function
	HashMap<String, String> exprFuncMap = new HashMap<>();

	protected String getFuncName(Expression e) {
		if (e instanceof PNonTerminal) {
			String uname = ((PNonTerminal) e).getUniqueName();
			if (uname.indexOf('"') > 0) {
				String funcName = this.symbolMap.get(uname);
				if (funcName == null) {
					funcName = "t" + this.symbolMap.size();
					this.symbolMap.put(uname, funcName);
				}
				return funcName;
			}
			return uname.replace(':', '_').replace('.', '_').replace('&', '_');
		}
		String key = e.toString();
		String name = this.exprFuncMap.get(key);
		if (name == null) {
			name = "e" + this.exprFuncMap.size();
			this.exprFuncMap.put(key, name);
		}
		return name;
	}

	HashSet<String> crossRefNames = new HashSet<>();
	HashMap<String, HashSet<String>> depsMap = new HashMap<>();
	// HashMap<String, Integer> memoPointMap = new HashMap<>();

	protected final void addFunctionDependency(String sour, String dest) {
		if (sour != null) {
			HashSet<String> set = this.depsMap.get(sour);
			if (set == null) {
				set = new HashSet<>();
				this.depsMap.put(sour, set);
			}
			set.add(dest);
		}
	}

	ArrayList<String> sortFuncList(String start) {
		class TopologicalSorter {
			private final HashMap<String, HashSet<String>> nodes;
			private final LinkedList<String> result;
			private final HashMap<String, Short> visited;
			private final Short Visiting = 1;
			private final Short Visited = 2;

			TopologicalSorter(HashMap<String, HashSet<String>> nodes) {
				this.nodes = nodes;
				this.result = new LinkedList<>();
				this.visited = new HashMap<>();
				for (Map.Entry<String, HashSet<String>> e : this.nodes.entrySet()) {
					if (this.visited.get(e.getKey()) == null) {
						this.visit(e.getKey(), e.getValue());
					}
				}
			}

			private void visit(String key, HashSet<String> nextNodes) {
				this.visited.put(key, this.Visiting);
				if (nextNodes != null) {
					for (String nextNode : nextNodes) {
						Short v = this.visited.get(nextNode);
						if (v == null) {
							this.visit(nextNode, this.nodes.get(nextNode));
						} else if (v == this.Visiting) {
							if (!key.equals(nextNode)) {
								// System.out.println("Cyclic " + key + " => " +
								// nextNode);
								CodeBase.this.crossRefNames.add(nextNode);
							}
						}
					}
				}
				this.visited.put(key, this.Visited);
				this.result.add(key);
			}

			public ArrayList<String> getResult() {
				return new ArrayList<>(this.result);
			}
		}
		TopologicalSorter sorter = new TopologicalSorter(this.depsMap);
		ArrayList<String> funcList = sorter.getResult();
		if (!funcList.contains(start)) {
			funcList.add(start);
		}
		this.depsMap.clear();
		return funcList;
	}

	// -------

	protected OCommonWriter out = new OCommonWriter();

}

class ParserGeneratorVisitor extends ExpressionVisitor<String, ParserGenerator> {
	final static boolean Function = true;
	final static boolean Inline = false;

	private ArrayList<Expression> waitingList = new ArrayList<>();

	public void start(ParserGrammar g, ParserGenerator px) {
		Production p = g.getStartProduction();
		px.defineConst("int", "MEMOSIZE", "" + g.getMemoPointSize());
		px.log("memosize: %d", g.getMemoPointSize());
		boolean isStateful = Stateful.isStateful(p);
		px.log("stateful: %s", isStateful);
		px.initGrammarProperty(g.isBinary(), isStateful);
		int c = 0;
		this.waitingList.add(p.getExpression());
		for (int i = 0; i < this.waitingList.size(); i++) {
			Expression e = this.waitingList.get(i);
			String funcName = px.getFuncName(e);
			MemoPoint memoPoint = null;
			if (e instanceof PNonTerminal) {
				memoPoint = g.getMemoPoint(((PNonTerminal) e).getUniqueName());
				e = ((PNonTerminal) e).getExpression();
			}
			if (!px.isDefined(funcName)) {
				px.openFragment(funcName);
				px.setCurrentFuncName(funcName);
				px.beginDefine(funcName, e);
				String pe = this.match(e, px, memoPoint);
				px.endDefine(funcName, pe);
				px.closeFlagment();
			}
			c++;
		}
		px.log("funcsize: %d", c);
	}

	public String match(Expression e, ParserGenerator px, MemoPoint m) {
		if (m == null) {
			return this.match(e, px, false);
		}
		boolean withTree = Typestate.compute(e) == Typestate.Tree;
		String combi = px.getMemoCombinator();
		if (combi != null) {
			if (withTree) {
				combi += "T";
			}
			String func = this.getInnerFunction(e, px);
			return px.callCombinator(combi, m.id, func);
		}
		int varid = px.uniqueVarId();
		String funcName = px.getFuncName(e);
		this.waitingList.add(e);
		px.addFunctionDependency(px.getCurrentFuncName(), funcName);
		String main = px.matchNonTerminal(funcName);
		main = px.matchPair(main, px.memoSucc(varid, m.id, withTree));
		main = px.matchChoice(main, px.memoFail(varid, m.id));
		return this.letVar(varid, POS, px.memoDispatch(px.memoLookup(m.id, withTree), main), px);
	}

	public String match(Expression e, ParserGenerator px, boolean asFunction) {
		if (asFunction) {
			String funcName = px.getFuncName(e);
			this.waitingList.add(e);
			px.addFunctionDependency(px.getCurrentFuncName(), funcName);
			return px.matchNonTerminal(funcName);
		} else {
			return e.visit(this, px);
		}
	}

	public String match(Expression e, ParserGenerator px) {
		String inline = null;
		if (e instanceof PRepetition) {
			inline = this.checkInlineRepetition((PRepetition) e, px);
		}
		if (e instanceof PNot) {
			inline = this.checkInlineNot((PNot) e, px);
		}
		if (e instanceof PLinkTree) {
			inline = this.checkInlineLink((PLinkTree) e, px);
		}
		if (e instanceof POption) {
			inline = this.checkInlineOption((POption) e, px);
		}
		if (e instanceof PAnd) {
			inline = this.checkInlineAnd((PAnd) e, px);
		}
		if (inline != null) {
			return inline;
		}
		return this.match(e, px, !this.isInline(e));
	}

	private boolean isInline(Expression e) {
		if (e instanceof PByte || e instanceof PByteSet || e instanceof PAny || e instanceof PTree
				|| e instanceof PPair) {
			return true;
		}
		if (e instanceof PTag || e instanceof PValue || e instanceof PEmpty || e instanceof PFail) {
			return true;
		}
		return false;
	}

	@Override
	public String visitNonTerminal(PNonTerminal e, ParserGenerator px) {
		String funcName = px.getFuncName(e);
		this.waitingList.add(e);
		px.addFunctionDependency(px.getCurrentFuncName(), funcName);
		return px.matchNonTerminal(funcName);
	}

	@Override
	public String visitEmpty(PEmpty e, ParserGenerator px) {
		return px.matchSucc();
	}

	@Override
	public String visitFail(PFail e, ParserGenerator px) {
		return px.matchFail();
	}

	@Override
	public String visitByte(PByte e, ParserGenerator px) {
		return px.matchByte(e.byteChar());
	}

	@Override
	public String visitByteSet(PByteSet e, ParserGenerator px) {
		return px.matchByteSet(e.byteSet());
	}

	@Override
	public String visitAny(PAny e, ParserGenerator px) {
		return px.matchAny();
	}

	@Override
	public String visitPair(PPair e, ParserGenerator px) {
		if (px.useMultiBytes()) {
			ArrayList<Integer> l = new ArrayList<>();
			Expression remain = Expression.extractMultiBytes(e, l);
			if (l.size() > 2) {
				byte[] text = Expression.toMultiBytes(l);
				String match = px.matchBytes(text);
				if (!(remain instanceof PEmpty)) {
					match = px.matchPair(match, this.match(remain, px));
				}
				return match;
			}
		}
		String pe1 = this.match(e.get(0), px);
		String pe2 = this.match(e.get(1), px);
		return px.matchPair(pe1, pe2);
	}

	final static int POS = 1;
	final static int TREE = 1 << 1;
	final static int STATE = 1 << 2;
	final static int CNT = 1 << 3;

	int flag(Expression e) {
		int flag = POS;
		if (Typestate.compute(e) != Typestate.Unit) {
			flag |= TREE;
		}
		if (Stateful.isStateful(e)) {
			flag |= STATE;
		}
		return flag;
	}

	String letVar(int varid, int flag, String pe, ParserGenerator px) {
		String code = "";
		if ((flag & POS) == POS) {
			code += px.initPosVar(varid);
		}
		if ((flag & TREE) == TREE) {
			code += px.initTreeVar(varid);
			code += px.initLogVar(varid);
		}
		if ((flag & STATE) == STATE) {
			code += px.initStateVar(varid);
		}
		if ((flag & CNT) == CNT) {
			code += px.initCountVar(varid);
		}
		code += pe;
		return code;
	}

	private String update(int varid, int flag, ParserGenerator px) {
		String code = "";
		if ((flag & POS) == POS) {
			code += px.updatePos(varid);
		}
		if ((flag & TREE) == TREE) {
			code += px.updateTree(varid);
			code += px.updateTreeLog(varid);
		}
		if ((flag & STATE) == STATE) {
			code += px.updateState(varid);
		}
		if ((flag & CNT) == CNT) {
			code += px.updateCountVar(varid);
		}
		return code;
	}

	String backtrack(int varid, int flag, ParserGenerator px) {
		if ((flag & POS) == POS) {
			if ((flag & STATE) == STATE) {
				if ((flag & TREE) == TREE) {
					return px.back4(varid);
				} else {
					return px.back2(varid);
				}
			} else {
				if ((flag & TREE) == TREE) {
					return px.back3(varid);
				} else {
					return px.backPos(varid);
				}
			}
		} else {
			if ((flag & TREE) == TREE) {
				return px.matchPair(px.backTree(varid), px.backTreeLog(varid));
			}
			return px.backState(varid);
		}
	}

	@Override
	public String visitChoice(PChoice e, ParserGenerator px) {
		int varid = px.uniqueVarId();
		int flag = this.flag(e);
		String first = this.match(e.get(0), px);
		for (int i = 1; i < e.size(); i++) {
			String second = px.matchPair(this.backtrack(varid, flag, px), this.match(e.get(i), px));
			first = px.matchChoice(first, second);
		}
		return this.letVar(varid, flag, px.result(first), px);
	}

	@Override
	public String visitDispatch(PDispatch e, ParserGenerator px) {
		String[] exprs = new String[e.size()];
		for (int i = 0; i < e.size(); i++) {
			exprs[i] = this.patch(e.get(i), px); // this.match(e.get(i), px);
		}
		return px.matchCase(px.fetchJumpIndex(e.indexMap), exprs);
	}

	private String patch(Expression e, ParserGenerator px) {
		if (e instanceof PPair) {
			Expression first = e.get(0);
			if (first instanceof PAny || first instanceof PByte || first instanceof PByteSet) {
				return px.matchPair(px.move(1), this.match(e.get(1), px));
			}
		}
		return this.match(e, px);
	}

	@Override
	public String visitOption(POption e, ParserGenerator px) {
		String pe = this.checkInlineOption(e, px);
		if (pe == null) {
			int varid = px.uniqueVarId();
			int flag = this.flag(e.get(0));
			String main = px.matchChoice(this.match(e.get(0), px), this.backtrack(varid, flag, px));
			return this.letVar(varid, flag, px.result(main), px);
		}
		return pe;
	}

	String checkInlineOption(POption e, ParserGenerator px) {
		if (px.useLexicalOptimization()) {
			if (e.get(0) instanceof PByte) {
				return px.matchOption(((PByte) e.get(0)).byteSet());
			}
			if (e.get(0) instanceof PByteSet) {
				return px.matchOption(((PByteSet) e.get(0)).byteSet());
			}
		}
		String combi = this.getCombinator(px.getOptionCombinator(), this.flag(e.get(0)));
		String innerFunc = this.getInnerFunction(e.get(0), px);
		if (combi != null && innerFunc != null) {
			return px.callCombinator(combi, innerFunc);
		}
		return null;
	}

	@Override
	public String visitRepetition(PRepetition e, ParserGenerator px) {
		String inline = this.checkInlineRepetition(e, px);
		if (inline == null) {
			int varid = px.uniqueVarId();
			int flag = this.flag(e.get(0));
			String cond = this.match(e.get(0), px);
			if (!NonEmpty.isAlwaysConsumed(e.get(0))) {
				cond = px.matchPair(cond, px.checkNonEmpty(varid));
			}
			String back = this.backtrack(varid, flag, px);
			if (e.isOneMore()) {
				flag |= CNT;
				back = px.matchPair(px.checkCountVar(varid), back);
			}
			String main = px.matchLoop(cond, this.update(varid, flag, px), back);
			return this.letVar(varid, flag, main, px);
		}
		return inline;
	}

	String checkInlineRepetition(PRepetition e, ParserGenerator px) {
		if (px.useLexicalOptimization()) {
			if (e.get(0) instanceof PByte) {
				return px.matchRepetition(((PByte) e.get(0)).byteSet());
			}
			if (e.get(0) instanceof PByteSet) {
				return px.matchRepetition(((PByteSet) e.get(0)).byteSet());
			}
		}
		String combi = this.getCombinator(px.getRepetitionCombinator(), this.flag(e.get(0)));
		String innerFunc = this.getInnerFunction(e.get(0), px);
		if (combi != null && innerFunc != null) {
			String zeroMore = px.callCombinator(combi, innerFunc);
			if (e.isOneMore()) {
				return px.matchPair(this.match(e.get(0), px), zeroMore);
			}
			return zeroMore;
		}
		return null;
	}

	@Override
	public String visitAnd(PAnd e, ParserGenerator px) {
		String inline = this.checkInlineAnd(e, px);
		if (inline == null) {
			int varid = px.uniqueVarId();
			String main = px.matchPair(this.match(e.get(0), px), this.backtrack(varid, POS, px));
			return this.letVar(varid, POS, px.result(main), px);
		}
		return inline;
	}

	String checkInlineAnd(PAnd e, ParserGenerator px) {
		if (px.useLexicalOptimization()) {
			if (e.get(0) instanceof PByte) {
				return px.matchAnd(((PByte) e.get(0)).byteSet());
			}
			if (e.get(0) instanceof PByteSet) {
				return px.matchAnd(((PByteSet) e.get(0)).byteSet());
			}
		}
		String combi = px.getAndCombinator();
		String innerFunc = this.getInnerFunction(e.get(0), px);
		if (combi != null && innerFunc != null) {
			return px.callCombinator(combi, innerFunc);
		}
		return null;
	}

	@Override
	public String visitNot(PNot e, ParserGenerator px) {
		String inline = this.checkInlineNot(e, px);
		if (inline == null) {
			int varid = px.uniqueVarId();
			int flag = this.flag(e.get(0));
			String main = px.matchIf(this.match(e.get(0), px), px.matchFail(), this.backtrack(varid, flag, px));
			return this.letVar(varid, flag, px.result(main), px);
		}
		return inline;
	}

	String checkInlineNot(PNot e, ParserGenerator px) {
		if (px.useLexicalOptimization()) {
			if (e.get(0) instanceof PByte) {
				return px.matchNot(((PByte) e.get(0)).byteSet());
			}
			if (e.get(0) instanceof PByteSet) {
				return px.matchNot(((PByteSet) e.get(0)).byteSet());
			}
		}
		String combi = this.getCombinator(px.getNotCombinator(), this.flag(e.get(0)));
		String innerFunc = this.getInnerFunction(e.get(0), px);
		if (combi != null && innerFunc != null) {
			return px.callCombinator(combi, innerFunc);
		}
		return null;
	}

	private String getCombinator(String suffix, int flag) {
		if (suffix != null) {
			if ((flag & STATE) == STATE) {
				return suffix + "TS";
			}
			if ((flag & TREE) == TREE) {
				return suffix + "T";
			}
		}
		return suffix;
	}

	private String getInnerFunction(Expression inner, ParserGenerator px) {
		if (px.supportedLambdaFunction() && this.isInline(inner)) {
			return px.defineLambda(this.match(inner, px, false));
		}
		this.match(inner, px, true);
		return px.refFunc(px.getFuncName(inner));
	}

	@Override
	public String visitTree(PTree e, ParserGenerator px) {
		String pe = px.matchPair(this.match(e.get(0), px), px.endTree(e.endShift, e.tag, e.value));
		if (e.folding) {
			return px.matchPair(px.foldTree(e.beginShift, e.label), pe);
		} else {
			return px.matchPair(px.beginTree(e.beginShift), pe);
		}
	}

	@Override
	public String visitDetree(PDetree e, ParserGenerator px) {
		int varid = px.uniqueVarId();
		String main = px.matchPair(this.match(e.get(0), px), this.backtrack(varid, TREE, px));
		return this.letVar(varid, TREE, px.result(main), px);
	}

	@Override
	public String visitLinkTree(PLinkTree e, ParserGenerator px) {
		String inline = this.checkInlineLink(e, px);
		if (inline == null) {
			int varid = px.uniqueVarId();
			String main = px.matchPair(this.match(e.get(0), px), px.backLink(varid, e.label));
			return this.letVar(varid, TREE, px.result(main), px);
		}
		return inline;
	}

	String checkInlineLink(PLinkTree e, ParserGenerator px) {
		String combi = px.getLinkCombinator();
		String innerFunc = this.getInnerFunction(e.get(0), px);
		if (combi != null && innerFunc != null) {
			return px.callCombinator(combi, e.label, innerFunc);
		}
		return null;
	}

	@Override
	public String visitTag(PTag e, ParserGenerator px) {
		return px.tagTree(e.tag);
	}

	@Override
	public String visitReplace(PValue e, ParserGenerator px) {
		return px.valueTree(e.value);
	}

	@Override
	public String visitSymbolScope(PSymbolScope e, ParserGenerator px) {
		int varid = px.uniqueVarId();
		String main = px.matchPair(this.match(e.get(0), px), this.backtrack(varid, STATE, px));
		if (e.label != null) {
			main = px.matchPair(px.callAction(new SymbolReset(), e.label, null), main);
		}
		return this.letVar(varid, STATE, px.result(main), px);
	}

	@Override
	public String visitSymbolAction(PSymbolAction e, ParserGenerator px) {
		if (e.isEmpty()) {
			return px.callAction(e.action, e.label, e.thunk);
		} else {
			int varid = px.uniqueVarId();
			String main = px.matchPair(this.match(e.get(0), px), px.callAction(e.action, e.label, varid, e.thunk));
			return this.letVar(varid, POS, px.result(main), px);
		}
	}

	@Override
	public String visitSymbolPredicate(PSymbolPredicate e, ParserGenerator px) {
		if (e.isEmpty()) {
			return px.callPredicate(e.pred, e.label, e.thunk);
		} else {
			int varid = px.uniqueVarId();
			String main = px.matchPair(this.match(e.get(0), px), px.callPredicate(e.pred, e.label, varid, e.thunk));
			return this.letVar(varid, POS, px.result(main), px);
		}
	}

	@Override
	public String visitIf(PIfCondition e, ParserGenerator px) {
		// Symbol label = Symbol.unique(e.flagName());
		// boolean b = new SymbolExist().match(px, label, px.pos, null);
		// return e.isPositive() ? b : !b;
		return px.matchSucc();
	}

	@Override
	public String visitOn(POnCondition e, ParserGenerator px) {
		// Symbol label = Symbol.unique(e.flagName());
		// Object state = px.loadSymbolTable();
		// if (e.isPositive()) {
		// new SymbolDefinition().mutate(px, label, px.pos);
		// } else {
		// new SymbolReset().mutate(px, label, px.pos);
		// }
		// if (!this.parse(e.get(0), px)) {
		// return false;
		// }
		// px.storeSymbolTable(state);
		// return true;
		return px.matchSucc();
	}

	@Override
	public String visitScan(PScan e, ParserGenerator px) {
		// int ppos = px.pos;
		// if (!this.parse(e.get(0), px)) {
		// return false;
		// }
		// px.scanCount(ppos, e.mask, e.shift);
		return px.matchSucc();
	}

	@Override
	public String visitRepeat(PRepeat e, ParserGenerator px) {
		// // int ppos = px.pos;
		// while (this.parse(e.get(0), px)) {
		// if (!px.decCount()) {
		// return true;
		// }
		// }
		return px.matchSucc();
	}

	@Override
	public String visitTrap(PTrap e, ParserGenerator px) {
		return px.matchSucc();
	}

}
