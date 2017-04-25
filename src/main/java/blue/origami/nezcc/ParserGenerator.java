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
import blue.nez.parser.ParserOption;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.Grammar;
import blue.nez.peg.NonEmpty;
import blue.nez.peg.Production;
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
import blue.nez.peg.expression.PReplace;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.origami.util.OCommonWriter;
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
		this.writeHeader();
		ArrayList<String> funcList = this.sortFuncList("start");
		for (String funcName : this.crossRefNames) {
			this.definePrototype(funcName);
		}
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

	protected abstract void writeHeader() throws IOException;

	protected abstract void writeFooter() throws IOException;

	@Override
	protected abstract void defineConst(String typeName, String constName, String literal);

	protected abstract void definePrototype(String funcName);

	protected abstract void beginDefine(String funcName, Expression e);

	protected abstract void endDefine(String funcName, String pe);

	protected abstract String result(String pe);

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

	protected abstract String backSymbolTable(int varid);

	protected abstract String updatePos(int varid);

	protected abstract String updateTree(int varid);

	protected abstract String updateTreeLog(int varid);

	protected abstract String updateSymbolTable(int varid);

	protected abstract String initCountVar(int varid);

	protected abstract String updateCountVar(int varid);

	protected abstract String tagTree(Symbol tag);

	protected abstract String valueTree(String value);

	protected abstract String linkTree(int varid, Symbol label);

	protected abstract String foldTree(int beginShift, Symbol label);

	protected abstract String beginTree(int beginShift);

	protected abstract String endTree(int endShift, Symbol tag, String value);

	protected abstract String callSymbolAction(SymbolAction action, Symbol label);

	protected abstract String callSymbolAction(SymbolAction action, Symbol label, int varid);

	protected abstract String callSymbolPredicate(SymbolPredicate pred, Symbol label, Object option);

	protected abstract String callSymbolPredicate(SymbolPredicate pred, Symbol label, int varid, Object option);

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
		return this.matchPair(this.backPos(varid), this.backSymbolTable(varid));
	}

	protected String back3(int varid) {
		String pe = this.matchPair(this.backTree(varid), this.backTreeLog(varid));
		return this.matchPair(this.backPos(varid), pe);
	}

	protected String back4(int varid) {
		return this.matchPair(this.back3(varid), this.backSymbolTable(varid));
	}

	protected String backLink(int varid, Symbol label) {
		String pe = this.matchPair(this.linkTree(varid, label), this.backTree(varid));
		return this.matchPair(this.backTreeLog(varid), pe);
	}

	protected String matchBytes(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		int c = 0;
		for (byte ch : bytes) {
			if (c > 0) {
				sb.append(this.s("&&"));
			}
			sb.append(this.matchByte(ch & 0xff));
			c++;
		}
		return sb.toString();
	}

	protected boolean useLexicalOptimization() {
		return false;
	}

	protected boolean useCombinator() {
		return false;
	}

	protected String getCombinator(Expression e) {
		// TODO Auto-generated method stub
		return null;
	}

	protected String matchCombinator(String combi, String funcName) {
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

	protected void initHeader() {
		this.head = new SourceFragment();
	}

	private SourceFragment body;

	protected String Line(String fmt, Object... args) {
		return this.body.Line(fmt, args);
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
		this.body = null;
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
					funcName = "c" + this.symbolMap.size();
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

		this.waitingList.add(p.getExpression());
		for (int i = 0; i < this.waitingList.size(); i++) {
			Expression e = this.waitingList.get(i);
			String funcName = px.getFuncName(e);
			if (e instanceof PNonTerminal) {
				e = ((PNonTerminal) e).getExpression();
			}
			if (!px.isDefined(funcName)) {
				px.openFragment(funcName);
				px.setCurrentFuncName(funcName);
				px.beginDefine(funcName, e);
				String pe = this.match(e, px, false);
				if (pe == null) {
					System.out.println("DEBUG " + funcName + " " + e);
				}
				px.endDefine(funcName, pe);
				px.closeFlagment();
			}
		}
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
		return this.match(e, px, !this.isInline(e));
	}

	private boolean isInline(Expression e) {
		if (e instanceof PByte || e instanceof PByteSet || e instanceof PAny || e instanceof PTree) {
			return true;
		}
		if (e instanceof PTag || e instanceof PReplace || e instanceof PEmpty || e instanceof PFail) {
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
		String pe1 = this.match(e.get(0), px);
		String pe2 = this.match(e.get(1), px);
		// if (pe1 == null) {
		// System.out.println("DEBUG " + e.get(0));
		// }
		// if (pe2 == null) {
		// System.out.println("DEBUG " + e.get(1));
		// }
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
			code += px.updateSymbolTable(varid);
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
			return px.backSymbolTable(varid);
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
		// px.loadPos(varid);
		// if (this.isCons(e)) {
		// px.loadTree(varid);
		// px.loadTreeLog(varid);
		// }
		// if (this.isStateful(e)) {
		// px.loadSymbolTable(varid);
		// }
		// px.beginIfSucc(this.match2(e.get(0), px));
		// px.jumpSucc();
		// px.endIf();
		// for (int i = 1; i < e.size(); i++) {
		// px.storePos(varid);
		// if (this.isCons(e)) {
		// px.storeTree(varid);
		// px.storeTreeLog(varid);
		// }
		// if (this.isStateful(e)) {
		// px.storeSymbolTable(varid);
		// }
		// px.beginIfSucc(this.match2(e.get(i), px));
		// px.jumpSucc();
		// px.endIf();
		// }
		// px.jumpFail();
		// }
	}

	@Override
	public String visitDispatch(PDispatch e, ParserGenerator px) {
		// int varid = px.uniqueVarId();
		String[] exprs = new String[e.size()];
		for (int i = 0; i < e.size(); i++) {
			exprs[i] = this.match(e.get(i), px);
		}
		return px.matchCase(px.fetchJumpIndex(e.indexMap), exprs);
		// String funcMap = px.getFuncMap(e);
		// if (funcMap != null) {
		// for (Expression sub : e) {
		// this.waitingList.add(sub);
		// }
		// return px.matchFuncMap(funcMap, px.fetchJumpIndex(e.indexMap));
		// } else {
		// int varid = px.uniqueVarId();
		// String[] exprs = new String[e.size()];
		// for (int i = 0; i < e.size(); i++) {
		// exprs[i] = this.match(e.get(i), px);
		// }
		// return px.matchCase(px.fetchJumpIndex(e.indexMap), exprs);
		// px.beginSwitch();
		// // px.beginCase(varid, 0);
		// // px.jumpFail();
		// // px.endCase();
		// for (int i = 0; i < e.size(); i++) {
		// px.beginCase(varid, i + 1);
		// px.jump(this.match(e.get(i), px));
		// px.endCase();
		// }
		// px.endSwitch();
		// px.jumpFail();
		// return null;
		// }
	}

	@Override
	public String visitOption(POption e, ParserGenerator px) {
		int varid = px.uniqueVarId();
		int flag = this.flag(e.get(0));
		String main = px.matchChoice(this.match(e.get(0), px), this.backtrack(varid, flag, px));
		return this.letVar(varid, flag, px.result(main), px);
		// px.loadPos(varid);
		// if (this.isCons(e.get(0))) {
		// px.loadTree(varid);
		// px.loadTreeLog(varid);
		// }
		// if (this.isStateful(e.get(0))) {
		// px.loadSymbolTable(varid);
		// }
		// px.beginIfFail(this.match2(e.get(0), px));
		// px.storePos(varid);
		// if (this.isCons(e.get(0))) {
		// px.storeTree(varid);
		// px.storeTreeLog(varid);
		// }
		// if (this.isStateful(e.get(0))) {
		// px.storeSymbolTable(varid);
		// }
		// px.endIf();
		// px.jumpSucc();
		// return null;
	}

	@Override
	public String visitRepetition(PRepetition e, ParserGenerator px) {
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

		// px.loadPos(varid);
		// if (this.isCons(e.get(0))) {
		// px.loadTree(varid);
		// px.loadTreeLog(varid);
		// }
		// if (this.isStateful(e.get(0))) {
		// px.loadSymbolTable(varid);
		// }
		// if (e.isOneMore()) {
		// px.initCounter(varid);
		// }
		// px.beginWhileSucc(this.match2(e.get(0), px));
		// {
		// // px.checkEmpty(varid, e.get(0));
		// px.updatePos(varid);
		// if (this.isCons(e.get(0))) {
		// px.updateTree(varid);
		// px.updateTreeLog(varid);
		// }
		// if (this.isStateful(e.get(0))) {
		// px.updateSymbolTable(varid);
		// }
		// if (e.isOneMore()) {
		// px.countCounter(varid);
		// }
		// }
		// px.endWhile();
		// if (e.isOneMore()) {
		// px.checkCounter(varid);
		// }
		// px.storePos(varid);
		// if (this.isCons(e.get(0))) {
		// px.storeTree(varid);
		// px.storeTreeLog(varid);
		// }
		// if (this.isStateful(e.get(0))) {
		// px.storeSymbolTable(varid);
		// }
		// px.jumpSucc();
		// return null;
	}

	@Override
	public String visitAnd(PAnd e, ParserGenerator px) {
		int varid = px.uniqueVarId();
		String main = px.matchPair(this.match(e.get(0), px), this.backtrack(varid, POS, px));
		return this.letVar(varid, POS, px.result(main), px);

		// px.loadPos(varid);
		// if (this.isCons(e.get(0))) {
		// px.loadTree(varid);
		// }
		// px.beginIfSucc(this.match2(e.get(0), px));
		// px.storePos(varid);
		// if (this.isCons(e.get(0))) {
		// px.storeTree(varid);
		// }
		// px.jumpSucc();
		// px.endIf();
		// px.jumpFail();
		// return null;
	}

	@Override
	public String visitNot(PNot e, ParserGenerator px) {
		int varid = px.uniqueVarId();
		int flag = this.flag(e.get(0));
		String main = px.matchIf(this.match(e.get(0), px), px.matchFail(), this.backtrack(varid, flag, px));
		return this.letVar(varid, flag, px.result(main), px);
		// int varid = px.uniqueVarId();
		// px.loadPos(varid);
		// if (this.isCons(e.get(0))) {
		// px.loadTree(varid);
		// px.loadTreeLog(varid);
		// }
		// if (this.isStateful(e.get(0))) {
		// px.loadSymbolTable(varid);
		// }
		// px.beginIfSucc(this.match2(e.get(0), px));
		// px.jumpFail();
		// px.orElse();
		// px.storePos(varid);
		// if (this.isCons(e.get(0))) {
		// px.storeTree(varid);
		// px.storeTreeLog(varid);
		// }
		// if (this.isStateful(e.get(0))) {
		// px.storeSymbolTable(varid);
		// }
		// px.jumpSucc();
		// px.endIf();
		// return null;
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
		// px.loadTree(varid);
		// px.loadTreeLog(varid);
		// px.beginIfSucc(this.match2(e.get(0), px));
		// px.storeTree(varid);
		// px.storeTreeLog(varid);
		// px.jumpSucc();
		// px.endIf();
		// px.jumpFail();
		// return null;
	}

	@Override
	public String visitLinkTree(PLinkTree e, ParserGenerator px) {
		int varid = px.uniqueVarId();
		String main = px.matchPair(this.match(e.get(0), px), px.backLink(varid, e.label));
		return this.letVar(varid, TREE, px.result(main), px);
		// if (this.isCons(e.get(0))) {
		// int varid = px.uniqueVarId();
		// px.initTreeVar(varid);
		// px.initLogVar(varid);
		// px.beginIfSucc(this.match2(e.get(0), px));
		// px.storeTreeLog(varid);
		// px.linkTree(varid, e.label);
		// px.storeTree(varid);
		// px.jumpSucc();
		// px.endIf();
		// px.jumpFail();
		// return null;
		// } else {
		// return this.match2(e.get(0), px);
		// }
	}

	@Override
	public String visitTag(PTag e, ParserGenerator px) {
		return px.tagTree(e.tag);
	}

	@Override
	public String visitReplace(PReplace e, ParserGenerator px) {
		return px.valueTree(e.value);
	}

	@Override
	public String visitSymbolScope(PSymbolScope e, ParserGenerator px) {
		int varid = px.uniqueVarId();
		String main = px.matchPair(this.match(e.get(0), px), this.backtrack(varid, STATE, px));
		if (e.label != null) {
			main = px.matchPair(px.callSymbolAction(new SymbolReset(), e.label), main);
		}
		return this.letVar(varid, STATE, px.result(main), px);

		// px.loadSymbolTable(varid);
		// px.beginIfSucc(this.match2(e.get(0), px));
		// px.storeSymbolTable(varid);
		// px.jumpSucc();
		// px.endIf();
		// px.jumpFail();
		// return null;
	}

	@Override
	public String visitSymbolAction(PSymbolAction e, ParserGenerator px) {
		if (e.isEmpty()) {
			return px.callSymbolAction(e.action, e.label);
		} else {
			int varid = px.uniqueVarId();
			String main = px.matchPair(this.match(e.get(0), px), px.callSymbolAction(e.action, e.label, varid));
			return this.letVar(varid, POS, px.result(main), px);
			// px.loadPos(varid);
			// px.beginIfSucc(this.match2(e.get(0), px));
			// px.jump(px.callSymbolAction(e.action, e.label, varid));
			// px.endIf();
			// px.jumpFail();
			// return null;
		}
	}

	@Override
	public String visitSymbolPredicate(PSymbolPredicate e, ParserGenerator px) {
		if (e.isEmpty()) {
			return px.callSymbolPredicate(e.pred, e.label, e.option);
		} else {
			int varid = px.uniqueVarId();
			String main = px.matchPair(this.match(e.get(0), px),
					px.callSymbolPredicate(e.pred, e.label, varid, e.option));
			return this.letVar(varid, POS, px.result(main), px);
			// px.loadPos(varid);
			// px.beginIfSucc(this.match2(e.get(0), px));
			// px.jump(px.callSymbolPredicate(e.pred, e.label, varid,
			// e.option));
			// px.endIf();
			// px.jumpFail();
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

class FunctionalParserGeneratorVisitor extends ParserGeneratorVisitor {
	String suffix(Expression e) {
		boolean isCons = Typestate.compute(e) != Typestate.Unit;
		return isCons ? "Tree" : "";
	}

	@Override
	public String visitOption(POption e, ParserGenerator px) {
		String combi = px.getCombinator(e);
		if (combi != null) {
			String funcName = this.match(e.get(0), px, Function);
			return px.matchCombinator(combi, funcName);
		}
		return super.visitOption(e, px);
	}

	@Override
	public String visitRepetition(PRepetition e, ParserGenerator px) {
		String combi = px.getCombinator(e);
		if (combi != null) {
			String funcName = this.match(e.get(0), px, Function);
			if (e.isOneMore()) {
				return px.matchPair(px.matchNonTerminal(funcName), px.matchCombinator(combi, funcName));
			}
			return px.matchCombinator(combi, funcName);
		}
		return super.visitRepetition(e, px);
	}

	@Override
	public String visitAnd(PAnd e, ParserGenerator px) {
		String combi = px.getCombinator(e);
		if (combi != null) {
			String funcName = this.match(e.get(0), px, Function);
			return px.matchCombinator(combi, funcName);
		}
		return super.visitAnd(e, px);
	}

	@Override
	public String visitNot(PNot e, ParserGenerator px) {
		String combi = px.getCombinator(e);
		if (combi != null) {
			String funcName = this.match(e.get(0), px, Function);
			return px.matchCombinator(combi, funcName);
		}
		return super.visitNot(e, px);
	}

}
