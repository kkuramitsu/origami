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

package blue.origami.xlocal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import blue.nez.ast.Symbol;
import blue.nez.parser.Parser;
import blue.nez.parser.ParserCode;
import blue.nez.parser.ParserGrammar.MemoPoint;
import blue.nez.parser.ParserOption;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.Grammar;
import blue.nez.peg.NonEmpty;
import blue.nez.peg.Production;
import blue.nez.peg.Typestate;
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
import blue.nez.peg.expression.PValue;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;
import blue.origami.util.OCommonWriter;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;
import blue.origami.util.OStringUtils;
import blue.origami.util.OVerbose;

public abstract class OldParserGenerator
		extends OCommonWriter /* implements GrammarWriter */ {

	protected boolean verboseMode = true;
	protected boolean Optimization = true;
	protected boolean SSEOption = false;
	protected boolean BinaryGrammar = false;
	protected boolean UniqueNumberingSymbol = true;
	protected boolean SupportedSwitchCase = true;
	protected boolean SupportedDoWhile = true;
	protected boolean UsingBitmap = false;
	protected boolean SupportedRange = false;
	protected boolean SupportedMatch2 = false;
	protected boolean SupportedMatch3 = false;
	protected boolean SupportedMatch4 = false;
	protected boolean SupportedMatch5 = false;
	protected boolean SupportedMatch6 = false;
	protected boolean SupportedMatch7 = false;
	protected boolean SupportedMatch8 = false;

	//
	protected ParserCode<?> code;

	public void writeGrammar(OOption options, Grammar g) {
		try {
			// FIXME Just a hack
			this.fileBase = this.extractGrammarName((String) options.get(ParserOption.GrammarFile));
			Parser parser = g.newParser(options);
			this.code = (ParserCode<?>) parser.compile();
			this.initLanguageSpec();
			this.generateHeader(g);
			SymbolAnalysis constDecl = new SymbolAnalysis();
			constDecl.decl(g.getStartProduction());
			this.sortFuncList(this._funcname(g.getStartProduction()));
			this.generateSymbolTables();
			this.generatePrototypes();
			new ParserGeneratorVisitor().generate();
			this.generateFooter(g);
			// file.writeNewLine();
			// file.flush();
		} catch (Exception e) {
			ODebug.traceException(e);
		}
	}

	// FIXME Following is a temporary modification. Please FIX Grammar class to
	// be available grammar file name.
	private String extractGrammarName(String path) {
		int start = path.lastIndexOf('/') + 1;
		start = (start == -1) ? 0 : start;
		int end = path.lastIndexOf('.');
		end = (end == -1) ? path.length() : end;
		return path.substring(start, end);
	}

	protected abstract void generateHeader(Grammar g);

	protected abstract void generateFooter(Grammar g);

	protected void generatePrototypes() {

	}

	protected String _funcname(Production p) {
		return this._funcname(p.getUniqueName());
	}

	protected String _funcname(String uname) {
		return "p" + this._rename(uname);
	}

	protected String _rename(String name) {
		return name.replace("!", "NOT").replace("~", "_").replace("&", "AND");
	}

	/* Types */

	protected HashMap<String, String> typeMap = new HashMap<>();

	protected abstract void initLanguageSpec();

	protected void addType(String name, String type) {
		this.typeMap.put(name, type);
	}

	protected String type(String name) {
		return this.typeMap.get(name);
	}

	/* Symbols */

	protected HashMap<String, String> nameMap = new HashMap<>();

	protected ArrayList<String> tagList = new ArrayList<>(8);
	protected HashMap<String, Integer> tagMap = new HashMap<>();

	protected HashMap<String, Integer> labelMap = new HashMap<>();
	protected ArrayList<String> labelList = new ArrayList<>(8);

	protected ArrayList<String> tableList = new ArrayList<>(8);
	protected HashMap<String, Integer> tableMap = new HashMap<>();

	final String _set(boolean[] b) {
		String key = OStringUtils.stringfyBitmap(b);
		String v = this.nameMap.get(key);
		return v;
	}

	final String _range(boolean[] b) {
		String key = OStringUtils.stringfyBitmap(b) + "*";
		String v = this.nameMap.get(key);
		return v;
	}

	final void DeclSet(boolean[] b, boolean Iteration) {
		if (Iteration && this.SSEOption) {
			byte[] range = this.rangeSEE(b);
			if (range != null) {
				String key = OStringUtils.stringfyBitmap(b) + "*";
				String name = this.nameMap.get(key);
				if (name == null) {
					name = this._range() + this.nameMap.size();
					this.nameMap.put(key, name);
					this.DeclConst(this.type("$range"), name, range.length, this._initByteArray(range));
				}
				return;
			}
		}
		if (this.SupportedRange && this.range(b) != null) {
			return;
		}
		String key = OStringUtils.stringfyBitmap(b);
		String name = this.nameMap.get(key);
		if (name == null) {
			name = this._set() + this.nameMap.size();
			this.nameMap.put(key, name);
			this.DeclConst(this.type("$set"), name, this.UsingBitmap ? 8 : 256, this._initBooleanArray(b));
		}
	}

	final String _index(byte[] b) {
		String key = this.key(b);
		return this.nameMap.get(key);
	}

	final void DeclIndex(byte[] b) {
		String key = this.key(b);
		String name = this.nameMap.get(key);
		if (name == null) {
			name = this._index() + this.nameMap.size();
			this.nameMap.put(key, name);
			this.DeclConst(this.type("$index"), name, b.length, this._initByteArray(b));
		}
	}

	private String key(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for (byte c : b) {
			sb.append(c);
			sb.append(",");
		}
		return sb.toString();
	}

	protected String _text(byte[] text) {
		String key = new String(text);
		return this.nameMap.get(key);
	}

	protected String _text(String key) {
		if (key == null) {
			return this._Null();
		}
		return this.nameMap.get(key);
	}

	final void DeclText(byte[] text) {
		String key = new String(text);
		String name = this.nameMap.get(key);
		if (name == null) {
			name = this._text() + this.nameMap.size();
			this.nameMap.put(key, name);
			this.DeclConst(this.type("$text"), name, text.length, this._initByteArray(text));
		}
	}

	private int[] range(boolean[] b) {
		int start = 0;
		for (int i = 0; i < 256; i++) {
			if (b[i]) {
				start = i;
				break;
			}
		}
		int end = 256;
		for (int i = start; i < 256; i++) {
			if (!b[i]) {
				end = i;
				break;
			}
		}
		for (int i = end; i < 256; i++) {
			if (b[i]) {
				return null;
			}
		}
		if (start < end) {
			int[] a = { start, end };
			return a;
		}
		return null;
	}

	private byte[] rangeSEE(boolean[] b) {
		if (b[0]) {
			return null;
		}
		ArrayList<Integer> l = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			if (b[i] == false) {
				int start = i;
				int end = start;
				for (int j = start; j < 256 && b[j] == false; j++) {
					end = j;
				}
				l.add(start);
				l.add(end);
				i = end;
			}
		}
		if (l.size() <= 16) {
			byte[] res = new byte[l.size()];
			for (int i = 0; i < l.size(); i++) {
				res[i] = (byte) ((int) l.get(i));
			}
			return res;
		}
		return null;
	}

	private boolean isMatchText(byte[] t, int n) {
		if (t.length == n) {
			for (int i = 0; i < n; i++) {
				if (t[i] == 0) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	final void DeclMatchText(byte[] text) {
		if ((this.SupportedMatch2 && this.isMatchText(text, 2)) || (this.SupportedMatch3 && this.isMatchText(text, 3))
				|| (this.SupportedMatch4 && this.isMatchText(text, 4))
				|| (this.SupportedMatch5 && this.isMatchText(text, 5))
				|| (this.SupportedMatch6 && this.isMatchText(text, 6))
				|| (this.SupportedMatch7 && this.isMatchText(text, 7))
				|| (this.SupportedMatch8 && this.isMatchText(text, 8))) {
			return;
		}
		this.DeclText(text);
	}

	final String _tag(Symbol s) {
		if (!this.UniqueNumberingSymbol && s == null) {
			return this._Null();
		}
		return this._tagname(s == null ? "" : s.getSymbol());
	}

	final void DeclTag(String s) {
		if (!this.tagMap.containsKey(s)) {
			int n = this.tagMap.size();
			this.tagMap.put(s, n);
			this.tagList.add(s);
			this.DeclConst(this.type("$tag"), this._tagname(s), this._initTag(n, s));
		}
	}

	final String _label(Symbol s) {
		if (!this.UniqueNumberingSymbol && s == null) {
			return this._Null();
		}
		return this._labelname(s == null ? "" : s.getSymbol());
	}

	final void DeclTrap(String s) {
		if (!this.labelMap.containsKey(s)) {
			int n = this.labelMap.size();
			this.labelMap.put(s, n);
			this.labelList.add(s);
			if (this.UniqueNumberingSymbol || !s.equals("_")) {
				this.DeclConst(this.type("$label"), this._labelname(s), this._initTrap(n, s));
			}
		}
	}

	final String _table(Symbol s) {
		if (!this.UniqueNumberingSymbol && s.equals("")) {
			return this._Null();
		}
		return this._tablename(s == null ? "" : s.getSymbol());
	}

	final void DeclTable(Symbol t) {
		String s = t.getSymbol();
		if (!this.tableMap.containsKey(s)) {
			int n = this.tableMap.size();
			this.tableMap.put(s, n);
			this.tableList.add(s);
			this.DeclConst(this.type("$table"), this._tablename(s), this._initTable(n, s));
		}
	}

	final void generateSymbolTables() {
		if (this.UniqueNumberingSymbol) {
			this.generateSymbolTable("_tags", this.tagList);
			this.generateSymbolTable("_labels", this.labelList);
			this.generateSymbolTable("_tables", this.tableList);
		}
	}

	private void generateSymbolTable(String name, ArrayList<String> l) {
		if (l.size() > 0) {
			this.DeclConst(this.type("$string"), name, l.size(), this._initStringArray(l, l.size()));
		}
	}

	String fileBase;

	protected String _basename() {
		return this.fileBase;
	}

	protected String _ns() {
		return this.fileBase + "_";
	}

	protected String _quote(String s) {
		if (s == null) {
			return "\"\"";
		}
		return OStringUtils.quoteString('"', s.toString(), '"');
	}

	protected String _initBooleanArray(boolean[] b) {
		StringBuilder sb = new StringBuilder();
		sb.append(this._BeginArray());
		// if (UsingBitmap) {
		// Bitmap bits = new Bitmap();
		// for (int i = 0; i < 256; i++) {
		// if (b[i]) {
		// bits.set(i, true);
		// assert (bits.is(i));
		// }
		// }
		// for (int i = 0; i < 8; i++) {
		// if (i > 0) {
		// sb.append(",");
		// }
		// sb.append(_hex(bits.n(i)));
		// }
		// } else {
		for (int i = 0; i < 256; i++) {
			if (i > 0) {
				sb.append(",");
			}
			if (b[i]) {
				sb.append(this._True());
			} else {
				sb.append(this._False());
			}
		}
		// }
		sb.append(this._EndArray());
		return sb.toString();
	}

	protected String _initByteArray(byte[] b) {
		StringBuilder sb = new StringBuilder();
		sb.append(this._BeginArray());
		for (int i = 0; i < b.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			// sb.append(_int(b[i] & 0xff));
			sb.append(b[i]);
		}
		sb.append(this._EndArray());
		return sb.toString();
	}

	protected String _initStringArray(List<String> a, int size) {
		StringBuilder sb = new StringBuilder();
		sb.append(this._BeginArray());
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(this._quote(a.get(i)));
		}
		sb.append(this._EndArray());
		return sb.toString();
	}

	protected String _tagname(String name) {
		return "_T" + name;
	}

	protected String _labelname(String name) {
		return "_L" + name;
	}

	protected String _tablename(String name) {
		return "_S" + name;
	}

	protected String _initTag(int id, String s) {
		return this.UniqueNumberingSymbol ? "" + id : this._quote(s);
	}

	protected String _initTrap(int id, String s) {
		return this.UniqueNumberingSymbol ? "" + id : this._quote(s);
	}

	protected String _initTable(int id, String s) {
		return this.UniqueNumberingSymbol ? "" + id : this._quote(s);
	}

	/* function */
	HashMap<String, String> exprMap = new HashMap<>();
	HashMap<String, Expression> funcMap = new HashMap<>();
	ArrayList<String> funcList = new ArrayList<>();
	HashSet<String> crossRefNames = new HashSet<>();
	HashMap<String, Integer> memoPointMap = new HashMap<>();

	private String _funcname(Expression e) {
		if (e instanceof PNonTerminal) {
			return this._funcname(((PNonTerminal) e).getUniqueName());
		}
		String key = e.toString();
		return this.exprMap.get(key);
	}

	HashMap<String, HashSet<String>> nodes = new HashMap<>();

	private void addEdge(String sour, String dest) {
		if (sour != null) {
			HashSet<String> set = this.nodes.get(sour);
			if (set == null) {
				set = new HashSet<>();
				this.nodes.put(sour, set);
			}
			set.add(dest);
		}
	}

	void sortFuncList(String start) {
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
								// Verbose.println("Cyclic " + key + " => " +
								// nextNode);
								OldParserGenerator.this.crossRefNames.add(nextNode);
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
		TopologicalSorter sorter = new TopologicalSorter(this.nodes);
		this.funcList = sorter.getResult();
		if (!this.funcList.contains(start)) {
			this.funcList.add(start);
		}
		this.nodes.clear();
	}

	private class SymbolAnalysis extends ExpressionVisitor<Object, Object> {

		SymbolAnalysis() {
			OldParserGenerator.this.DeclTag("");
			OldParserGenerator.this.DeclTrap("");
			OldParserGenerator.this.DeclTable(Symbol.Null);
		}

		private Object decl(Production p) {
			if (this.checkFuncName(p)) {
				p.getExpression().visit(this, null);
			}
			return null;
		}

		String cur = null; // name

		private boolean checkFuncName(Production p) {
			String f = OldParserGenerator.this._funcname(p.getUniqueName());
			if (!OldParserGenerator.this.funcMap.containsKey(f)) {
				String stacked2 = this.cur;
				this.cur = f;
				OldParserGenerator.this.funcMap.put(f, p.getExpression());
				OldParserGenerator.this.funcList.add(f);
				MemoPoint memoPoint = OldParserGenerator.this.code.getMemoPoint(p.getUniqueName());
				if (memoPoint != null) {
					OldParserGenerator.this.memoPointMap.put(f, memoPoint.id);
					String stacked = this.cur;
					this.cur = f;
					this.checkInner(p.getExpression());
					this.cur = stacked;
					OldParserGenerator.this.addEdge(this.cur, f);
				} else {
					p.getExpression().visit(this, null);
				}
				this.cur = stacked2;
				OldParserGenerator.this.addEdge(this.cur, f);
				return true;
			}
			OldParserGenerator.this.addEdge(this.cur, f);
			// crossRefNames.add(f);
			return false;
		}

		private void checkInner(Expression e) {
			if (e instanceof PNonTerminal) {
				e.visit(this, null);
				return;
			}
			String key = e.toString();
			String f = OldParserGenerator.this.exprMap.get(key);
			if (f == null) {
				f = "e" + OldParserGenerator.this.exprMap.size();
				OldParserGenerator.this.exprMap.put(key, f);
				OldParserGenerator.this.funcList.add(f);
				OldParserGenerator.this.funcMap.put(f, e);
				String stacked = this.cur;
				this.cur = f;
				e.visit(this, null);
				this.cur = stacked;
			}
			OldParserGenerator.this.addEdge(this.cur, f);
			// crossRefNames.add(f);
		}

		private void checkNonLexicalInner(Expression e) {
			if (OldParserGenerator.this.Optimization) {
				if (e instanceof PByte || e instanceof PByteSet || e instanceof PAny) {
					e.visit(this, null);
					return;
				}
			}
			this.checkInner(e);
		}

		private Object visitInnerAll(Expression e) {
			for (Expression sub : e) {
				sub.visit(this, null);
			}
			return null;
		}

		@Override
		public Object visitNonTerminal(PNonTerminal e, Object a) {
			return this.decl(e.getProduction());
		}

		@Override
		public Object visitEmpty(PEmpty e, Object a) {
			return null;
		}

		@Override
		public Object visitFail(PFail e, Object a) {
			return null;
		}

		@Override
		public Object visitByte(PByte e, Object a) {
			return null;
		}

		@Override
		public Object visitByteSet(PByteSet e, Object a) {
			OldParserGenerator.this.DeclSet(e.bools(), false);
			return null;
		}

		@Override
		public Object visitAny(PAny e, Object a) {
			return null;
		}

		@Override
		public Object visitPair(PPair e, Object a) {
			return this.visitInnerAll(e);
		}

		@Override
		public Object visitChoice(PChoice e, Object a) {
			// if (e.predicted != null) {
			// DeclIndex(e.predicted.indexMap);
			// }
			if (e.size() == 1) {
				// single selection
				e.get(0).visit(this, null);
			} else {
				for (Expression sub : e) {
					this.checkInner(sub);
				}
			}
			return null;
		}

		@Override
		public Object visitDispatch(PDispatch e, Object a) {
			OldParserGenerator.this.DeclIndex(e.indexMap);
			for (Expression sub : e) {
				this.checkInner(sub);
			}
			return null;
		}

		@Override
		public Object visitOption(POption e, Object a) {
			this.checkNonLexicalInner(e.get(0));
			return null;
		}

		@Override
		public Object visitRepetition(PRepetition e, Object a) {
			if (OldParserGenerator.this.Optimization && e.get(0) instanceof PByteSet) {
				OldParserGenerator.this.DeclSet(((PByteSet) e.get(0)).bools(), true);
				return null;
			}
			this.checkNonLexicalInner(e.get(0));
			return null;
		}

		@Override
		public Object visitAnd(PAnd e, Object a) {
			this.checkNonLexicalInner(e.get(0));
			return null;
		}

		@Override
		public Object visitNot(PNot e, Object a) {
			this.checkNonLexicalInner(e.get(0));
			return null;
		}

		// @Override
		// public Object visitBeginTree(Expression.BeginTree e, Object a) {
		// return null;
		// }

		@Override
		public Object visitTree(PTree e, Object a) {
			if (e.label != null) {
				OldParserGenerator.this.DeclTrap(e.label.getSymbol());
			}
			if (e.tag != null) {
				OldParserGenerator.this.DeclTag(e.tag.getSymbol());
			}
			if (e.value != null) {
				OldParserGenerator.this.DeclText(OStringUtils.utf8(e.value));
			}
			return this.visitInnerAll(e);
		}

		@Override
		public Object visitLinkTree(PLinkTree e, Object a) {
			if (e.label != null) {
				OldParserGenerator.this.DeclTrap(e.label.getSymbol());
			}
			return this.visitInnerAll(e);
		}

		@Override
		public Object visitTag(PTag e, Object a) {
			OldParserGenerator.this.DeclTag(e.tag.getSymbol());
			return null;
		}

		@Override
		public Object visitReplace(PValue e, Object a) {
			OldParserGenerator.this.DeclText(OStringUtils.utf8(e.value));
			return null;
		}

		@Override
		public Object visitDetree(PDetree e, Object a) {
			return this.visitInnerAll(e);
		}

		@Override
		public Object visitSymbolScope(PSymbolScope e, Object a) {
			return this.visitInnerAll(e);
		}

		@Override
		public Object visitSymbolAction(PSymbolAction e, Object a) {
			OldParserGenerator.this.DeclTable(e.label);
			return this.visitInnerAll(e);
		}

		@Override
		public Object visitSymbolPredicate(PSymbolPredicate e, Object a) {
			OldParserGenerator.this.DeclTable(e.label);
			return this.visitInnerAll(e);
		}

		// @Override
		// public Object visitSymbolMatch(Expression.SymbolMatch e, Object a) {
		// DeclTable(e.tableName);
		// return visitInnerAll(e);
		// }
		//
		// @Override
		// public Object visitSymbolExists(Expression.SymbolExists e, Object a)
		// {
		// DeclTable(e.tableName);
		// if (e.symbol != null) {
		// DeclText(StringUtils.utf8(e.symbol));
		// }
		// return visitInnerAll(e);
		// }

		@Override
		public Object visitScan(PScan e, Object a) {
			return this.visitInnerAll(e);
		}

		@Override
		public Object visitRepeat(PRepeat e, Object a) {
			return this.visitInnerAll(e);
		}

		@Override
		public Object visitIf(PIfCondition e, Object a) {
			return null;
		}

		@Override
		public Object visitOn(POnCondition e, Object a) {
			return this.visitInnerAll(e);
		}

		@Override
		public Object visitTrap(PTrap e, Object a) {
			return null;
		}
	}

	class ParserGeneratorVisitor extends ExpressionVisitor<Object, Object> {

		void generate() {
			for (String f : OldParserGenerator.this.funcList) {
				this.generateFunction(f, OldParserGenerator.this.funcMap.get(f));
			}
		}

		private String _eval(Expression e) {
			String f = OldParserGenerator.this._funcname(e);
			if (f == null) {
				return null;
			}
			return OldParserGenerator.this._funccall(f);
		}

		private String _eval(String uname) {
			return OldParserGenerator.this._funccall(OldParserGenerator.this._funcname(uname));
		}

		private void generateFunction(String name, Expression e) {
			Integer memoPoint = OldParserGenerator.this.memoPointMap.get(name);
			OldParserGenerator.this.pVerbose(e.toString());
			this.initLocal();
			OldParserGenerator.this.BeginFunc(name);
			{
				if (memoPoint != null) {
					String memoLookup = "memoLookupTree";
					String memoSucc = "memoTreeSucc";
					String memoFail = "memoFail";
					// String memoLookup = "memoLookupStateTree";
					// String memoSucc = "memoStateTreeSucc";
					// String memoFail = "memoStateFail";
					if (Typestate.compute(e) != Typestate.Tree) {
						memoLookup = memoLookup.replace("Tree", "");
						memoSucc = memoSucc.replace("Tree", "");
						memoFail = memoFail.replace("Tree", "");
					}
					// if (!strategy.StatefulPackratParsing ||
					// !symbolDeps.isDependent(e)) {
					// memoLookup = memoLookup.replace("State", "");
					// memoSucc = memoSucc.replace("State", "");
					// memoFail = memoFail.replace("State", "");
					// }
					this.InitVal("memo", OldParserGenerator.this._Func(memoLookup, OldParserGenerator.this._int(memoPoint)));
					OldParserGenerator.this.If("memo", OldParserGenerator.this._Eq(), "0");
					{
						String f = this._eval(e);
						String[] n = this.SaveState(e);
						OldParserGenerator.this.If(f);
						{
							OldParserGenerator.this.Statement(
									OldParserGenerator.this._Func(memoSucc, OldParserGenerator.this._int(memoPoint), n[0]));
							OldParserGenerator.this.Succ();
						}
						OldParserGenerator.this.Else();
						{
							this.BackState(e, n);
							OldParserGenerator.this.Statement(
									OldParserGenerator.this._Func(memoFail, OldParserGenerator.this._int(memoPoint)));
							OldParserGenerator.this.Fail();
						}
						OldParserGenerator.this.EndIf();
					}
					OldParserGenerator.this.EndIf();
					OldParserGenerator.this.Return(OldParserGenerator.this._Binary("memo", OldParserGenerator.this._Eq(), "1"));
				} else {
					this.visit(e, null);
					OldParserGenerator.this.Succ();
				}
			}
			OldParserGenerator.this.EndFunc();
		}

		void initFunc(Expression e) {

		}

		int nested = -1;

		private void visit(Expression e, Object a) {
			int lnested = this.nested;
			this.nested++;
			e.visit(this, a);
			this.nested--;
			this.nested = lnested;
		}

		protected void BeginScope() {
			if (this.nested > 0) {
				OldParserGenerator.this.BeginLocalScope();
			}
		}

		protected void EndScope() {
			if (this.nested > 0) {
				OldParserGenerator.this.EndLocalScope();
			}
		}

		HashMap<String, String> localMap;

		private void initLocal() {
			this.localMap = new HashMap<>();
		}

		private String local(String name) {
			if (!this.localMap.containsKey(name)) {
				this.localMap.put(name, name);
				return name;
			}
			return this.local(name + this.localMap.size());
		}

		private String InitVal(String name, String expr) {
			String type = OldParserGenerator.this.type(name);
			String lname = this.local(name);
			OldParserGenerator.this.VarDecl(type, lname, expr);
			return lname;
		}

		private String SavePos() {
			return this.InitVal(OldParserGenerator.this._pos(),
					OldParserGenerator.this._Field(OldParserGenerator.this._state(), "pos"));
		}

		private void BackPos(String lname) {
			OldParserGenerator.this.VarAssign(OldParserGenerator.this._Field(OldParserGenerator.this._state(), "pos"), lname);
		}

		private String SaveTree() {
			return this.InitVal(OldParserGenerator.this._tree(), OldParserGenerator.this._Func("saveTree"));
		}

		private void BackTree(String lname) {
			OldParserGenerator.this.Statement(OldParserGenerator.this._Func("backTree", lname));
		}

		private String SaveLog() {
			return this.InitVal(OldParserGenerator.this._log(), OldParserGenerator.this._Func("saveLog"));
		}

		private void BackLog(String lname) {
			OldParserGenerator.this.Statement(OldParserGenerator.this._Func("backLog", lname));
		}

		private String SaveSymbolTable() {
			return this.InitVal(OldParserGenerator.this._table(), OldParserGenerator.this._Func("saveSymbolPoint"));
		}

		private void BackSymbolTable(String lname) {
			OldParserGenerator.this.Statement(OldParserGenerator.this._Func("backSymbolPoint", lname));
		}

		private String[] SaveState(Expression inner) {
			String[] names = new String[4];
			names[0] = this.SavePos();
			if (Typestate.compute(inner) != Typestate.Unit) {
				names[1] = this.SaveTree();
				names[2] = this.SaveLog();
			}
			// FIXME: To Kayaban and Omega Celery
			// if (symbolMutation.isMutated(inner)) {
			// names[3] = SaveSymbolTable();
			// }
			return names;
		}

		private void BackState(Expression inner, String[] names) {
			this.BackPos(names[0]);
			if (names[1] != null) {
				this.BackTree(names[1]);
			}
			if (names[2] != null) {
				this.BackLog(names[2]);
			}
			if (names[3] != null) {
				this.BackSymbolTable(names[3]);
			}
		}

		@Override
		public Object visitNonTerminal(PNonTerminal e, Object a) {
			String f = this._eval(e.getUniqueName());
			OldParserGenerator.this.If(OldParserGenerator.this._Not(f));
			{
				OldParserGenerator.this.Fail();
			}
			OldParserGenerator.this.EndIf();
			return null;
		}

		@Override
		public Object visitEmpty(PEmpty e, Object a) {
			return null;
		}

		@Override
		public Object visitFail(PFail e, Object a) {
			OldParserGenerator.this.Fail();
			return null;
		}

		@Override
		public Object visitByte(PByte e, Object a) {
			OldParserGenerator.this.If(OldParserGenerator.this._Func("read"), OldParserGenerator.this._NotEq(),
					OldParserGenerator.this._byte(e.byteChar()));
			{
				OldParserGenerator.this.Fail();
			}
			OldParserGenerator.this.EndIf();
			this.checkBinaryEOF(e.byteChar() == 0);
			return null;
		}

		@Override
		public Object visitByteSet(PByteSet e, Object a) {
			boolean[] byteset = e.bools();
			OldParserGenerator.this.If(OldParserGenerator.this._Not(this.MatchByteArray(byteset, true)));
			{
				OldParserGenerator.this.Fail();
			}
			OldParserGenerator.this.EndIf();
			this.checkBinaryEOF(byteset[0]);
			return null;
		}

		private void checkBinaryEOF(boolean checked) {
			if (OldParserGenerator.this.BinaryGrammar && checked) {
				OldParserGenerator.this.If(OldParserGenerator.this._Func("eof"));
				{
					OldParserGenerator.this.Fail();
				}
				OldParserGenerator.this.EndIf();
			}
		}

		private String MatchByteArray(boolean[] byteMap, boolean inc) {
			String c = inc ? OldParserGenerator.this._Func("read") : OldParserGenerator.this._Func("prefetch");
			if (OldParserGenerator.this.SupportedRange) {
				int[] range = OldParserGenerator.this.range(byteMap);
				if (range != null) {
					String s = "("
							+ OldParserGenerator.this._Binary(
									OldParserGenerator.this._Binary(OldParserGenerator.this._byte(range[0]), "<=",
											OldParserGenerator.this._Func("prefetch")),
									OldParserGenerator.this._And(),
									OldParserGenerator.this._Binary(c, "<", OldParserGenerator.this._byte(range[1])))
							+ ")";
					// System.out.println(s);
					return s;
				}
			}
			if (OldParserGenerator.this.UsingBitmap) {
				return OldParserGenerator.this._Func("bitis", OldParserGenerator.this._set(byteMap), c);
			} else {
				return OldParserGenerator.this._GetArray(OldParserGenerator.this._set(byteMap), c);
			}
		}

		@Override
		public Object visitAny(PAny e, Object a) {
			if (OldParserGenerator.this.BinaryGrammar) {
				OldParserGenerator.this.Statement(OldParserGenerator.this._Func("move", "1"));
				OldParserGenerator.this.If(OldParserGenerator.this._Func("eof"));
				{
					OldParserGenerator.this.Fail();
				}
				OldParserGenerator.this.EndIf();
			} else {
				OldParserGenerator.this.If(OldParserGenerator.this._Func("read"), OldParserGenerator.this._Eq(), "0");
				{
					OldParserGenerator.this.Fail();
				}
				OldParserGenerator.this.EndIf();
			}
			return null;
		}

		// @Override
		// public Object visitMultiByte(Expression.MultiByte e, Object a) {
		// If(_Not(_Match(e.byteseq)));
		// {
		// Fail();
		// }
		// EndIf();
		// return null;
		// }

		@Override
		public Object visitPair(PPair e, Object a) {
			for (Expression sub : e) {
				this.visit(sub, a);
			}
			return null;
		}

		@Override
		public Object visitChoice(PChoice e, Object a) {
			// if (e.predicted != null && SupportedSwitchCase) {
			// generateSwitch(e, e.predicted);
			// } else {
			this.BeginScope();
			String temp = this.InitVal(OldParserGenerator.this._temp(), OldParserGenerator.this._True());
			for (Expression sub : e) {
				String f = this._eval(sub);
				OldParserGenerator.this.If(temp);
				{
					String[] n = this.SaveState(sub);
					OldParserGenerator.this.pVerbose(sub.toString());
					OldParserGenerator.this.If(f);
					{
						OldParserGenerator.this.VarAssign(temp, OldParserGenerator.this._False());
					}
					OldParserGenerator.this.Else();
					{
						this.BackState(sub, n);
					}
					OldParserGenerator.this.EndIf();
				}
				OldParserGenerator.this.EndIf();
			}
			OldParserGenerator.this.If(temp);
			{
				OldParserGenerator.this.Fail();
			}
			OldParserGenerator.this.EndIf();
			this.EndScope();
			// }
			return null;
		}

		// private void generateSwitch(Expression.Choice choice,
		// ChoicePrediction p) {
		// if (choice.size() == 1) {
		// Verbose.println("single choice: " + choice);
		// choice.get(0).visit(this, null);
		// } else {
		// String temp = InitVal(_temp(), _True());
		// Switch(_GetArray(_index(p.indexMap), _Func("prefetch")));
		// Case("0");
		// Fail();
		// for (int i = 0; i < choice.size(); i++) {
		// Case(_int(i + 1));
		// Expression sub = choice.get(i);
		// String f = _eval(sub);
		// if (p.striped[i]) {
		// Verbose(". " + sub);
		// Statement(_Func("move", "1"));
		// } else {
		// Verbose(sub.toString());
		// }
		// VarAssign(temp, f);
		// Break();
		// EndCase();
		// }
		// EndSwitch();
		// If(_Not(temp));
		// {
		// Fail();
		// }
		// EndIf();
		// }
		// }

		@Override
		public Object visitDispatch(PDispatch e, Object a) {
			String temp = this.InitVal(OldParserGenerator.this._temp(), OldParserGenerator.this._True());
			OldParserGenerator.this.Switch(OldParserGenerator.this._GetArray(OldParserGenerator.this._index(e.indexMap),
					OldParserGenerator.this._Func("prefetch")));
			OldParserGenerator.this.Case("0");
			OldParserGenerator.this.Fail();
			int caseIndex = 1;
			for (Expression sub : e) {
				OldParserGenerator.this.Case(OldParserGenerator.this._int(caseIndex));
				String f = this._eval(sub);
				OldParserGenerator.this.pVerbose(sub.toString());
				OldParserGenerator.this.VarAssign(temp, f);
				OldParserGenerator.this.Break();
				OldParserGenerator.this.EndCase();
				caseIndex++;
			}
			OldParserGenerator.this.EndSwitch();
			OldParserGenerator.this.If(OldParserGenerator.this._Not(temp));
			{
				OldParserGenerator.this.Fail();
			}
			OldParserGenerator.this.EndIf();
			return null;
		}

		@Override
		public Object visitOption(POption e, Object a) {
			Expression sub = e.get(0);
			if (!this.tryOptionOptimization(sub)) {
				String f = this._eval(sub);
				String[] n = this.SaveState(sub);
				OldParserGenerator.this.pVerbose(sub.toString());
				OldParserGenerator.this.If(OldParserGenerator.this._Not(f));
				{
					this.BackState(sub, n);
				}
				OldParserGenerator.this.EndIf();
			}
			return null;
		}

		@Override
		public Object visitRepetition(PRepetition e, Object a) {
			if (e.isOneMore()) {
				if (!this.tryRepetitionOptimization(e.get(0), true)) {
					String f = this._eval(e.get(0));
					if (f != null) {
						OldParserGenerator.this.If(OldParserGenerator.this._Not(f));
						{
							OldParserGenerator.this.Fail();
						}
						OldParserGenerator.this.EndIf();
					} else {
						this.visit(e.get(0), a);
					}
					this.generateWhile(e, a);
				}
			} else {
				if (!this.tryRepetitionOptimization(e.get(0), false)) {
					this.generateWhile(e, a);
				}
			}
			return null;
		}

		private void generateWhile(Expression e, Object a) {
			Expression sub = e.get(0);
			String f = this._eval(sub);
			OldParserGenerator.this.While(OldParserGenerator.this._True());
			{
				String[] n = this.SaveState(sub);
				OldParserGenerator.this.pVerbose(sub.toString());
				OldParserGenerator.this.If(OldParserGenerator.this._Not(f));
				{
					this.BackState(sub, n);
					OldParserGenerator.this.Break();
				}
				OldParserGenerator.this.EndIf();
				this.CheckInfiniteLoop(sub, n[0]);
			}
			OldParserGenerator.this.EndWhile();
		}

		private void CheckInfiniteLoop(Expression e, String var) {
			if (!NonEmpty.isAlwaysConsumed(e)) {
				OldParserGenerator.this.If(var, OldParserGenerator.this._Eq(),
						OldParserGenerator.this._Field(OldParserGenerator.this._state(), "pos"));
				{
					OldParserGenerator.this.Break();
				}
				OldParserGenerator.this.EndIf();
			}
		}

		@Override
		public Object visitAnd(PAnd e, Object a) {
			Expression sub = e.get(0);
			if (!this.tryAndOptimization(sub)) {
				String f = this._eval(sub);
				this.BeginScope();
				String n = this.SavePos();
				OldParserGenerator.this.pVerbose(sub.toString());
				OldParserGenerator.this.If(OldParserGenerator.this._Not(f));
				{
					OldParserGenerator.this.Fail();
				}
				OldParserGenerator.this.EndIf();
				this.BackPos(n);
				this.EndScope();
			}
			return null;
		}

		@Override
		public Object visitNot(PNot e, Object a) {
			Expression sub = e.get(0);
			if (!this.tryNotOptimization(sub)) {
				String f = this._eval(sub);
				this.BeginScope();
				String[] n = this.SaveState(sub);
				OldParserGenerator.this.pVerbose(sub.toString());
				OldParserGenerator.this.If(f);
				{
					OldParserGenerator.this.Fail();
				}
				OldParserGenerator.this.EndIf();
				this.BackState(sub, n);
				this.EndScope();
			}
			return null;
		}

		private boolean tryOptionOptimization(Expression inner) {
			if (OldParserGenerator.this.Optimization) {
				if (inner instanceof PByte) {
					PByte e = (PByte) inner;
					OldParserGenerator.this.If(OldParserGenerator.this._Func("prefetch"), OldParserGenerator.this._Eq(),
							OldParserGenerator.this._byte(e.byteChar()));
					{
						if (OldParserGenerator.this.BinaryGrammar && e.byteChar() == 0) {
							OldParserGenerator.this.If(OldParserGenerator.this._Not(OldParserGenerator.this._Func("eof")));
							{
								OldParserGenerator.this.Statement(OldParserGenerator.this._Func("move", "1"));
							}
							OldParserGenerator.this.EndIf();
						} else {
							OldParserGenerator.this.Statement(OldParserGenerator.this._Func("move", "1"));
						}
					}
					OldParserGenerator.this.EndIf();
					return true;
				}
				if (inner instanceof PByteSet) {
					PByteSet e = (PByteSet) inner;
					boolean[] byteset = e.bools();
					OldParserGenerator.this.If(this.MatchByteArray(byteset, false));
					{
						if (OldParserGenerator.this.BinaryGrammar && byteset[0]) {
							OldParserGenerator.this.If(OldParserGenerator.this._Not(OldParserGenerator.this._Func("eof")));
							{
								OldParserGenerator.this.Statement(OldParserGenerator.this._Func("move", "1"));
							}
							OldParserGenerator.this.EndIf();
						} else {
							OldParserGenerator.this.Statement(OldParserGenerator.this._Func("move", "1"));
						}
					}
					OldParserGenerator.this.EndIf();
					return true;
				}
				// if (inner instanceof Expression.MultiByte) {
				// Expression.MultiByte e = (Expression.MultiByte) inner;
				// Statement(_Match(e.byteseq));
				// return true;
				// }
				if (inner instanceof PAny) {
					// Expression.Any e = (Expression.Any) inner;
					OldParserGenerator.this.If(OldParserGenerator.this._Not(OldParserGenerator.this._Func("eof")));
					{
						OldParserGenerator.this.Statement(OldParserGenerator.this._Func("move", "1"));
					}
					OldParserGenerator.this.EndIf();
					return true;
				}
			}
			return false;
		}

		private boolean tryRepetitionOptimization(Expression inner, boolean isOneMore) {
			if (OldParserGenerator.this.Optimization) {
				if (inner instanceof PByte) {
					PByte e = (PByte) inner;
					if (isOneMore) {
						this.visit(inner, null);
					}
					OldParserGenerator.this.While(OldParserGenerator.this._Binary(OldParserGenerator.this._Func("prefetch"),
							OldParserGenerator.this._Eq(), OldParserGenerator.this._byte(e.byteChar())));
					{
						if (OldParserGenerator.this.BinaryGrammar && e.byteChar() == 0) {
							OldParserGenerator.this.If(OldParserGenerator.this._Func("eof"));
							{
								OldParserGenerator.this.Break();
							}
							OldParserGenerator.this.EndIf();
						}
						OldParserGenerator.this.Statement(OldParserGenerator.this._Func("move", "1"));
					}
					OldParserGenerator.this.EndWhile();
					return true;
				}
				if (inner instanceof PByteSet) {
					PByteSet e = (PByteSet) inner;
					boolean[] byteset = e.bools();
					if (OldParserGenerator.this.SSEOption) {
						String name = OldParserGenerator.this._range(byteset);
						if (name != null) {
							byte[] r = OldParserGenerator.this.rangeSEE(byteset);
							OVerbose.println("range: " + name + " " + e);
							if (isOneMore) {
								OldParserGenerator.this.If(OldParserGenerator.this._Not(OldParserGenerator.this
										._Func("checkOneMoreRange", name, OldParserGenerator.this._int(r.length))));
								{
									OldParserGenerator.this.Fail();
								}
								OldParserGenerator.this.EndIf();
							} else {
								OldParserGenerator.this.Statement(OldParserGenerator.this._Func("skipRange", name,
										OldParserGenerator.this._int(r.length)));
							}
							return true;
						}
					}
					if (isOneMore) {
						this.visit(inner, null);
					}
					OldParserGenerator.this.While(this.MatchByteArray(byteset, false));
					{
						if (OldParserGenerator.this.BinaryGrammar && byteset[0]) {
							OldParserGenerator.this.If(OldParserGenerator.this._Func("eof"));
							{
								OldParserGenerator.this.Break();
							}
							OldParserGenerator.this.EndIf();
						}
						OldParserGenerator.this.Statement(OldParserGenerator.this._Func("move", "1"));
					}
					OldParserGenerator.this.EndWhile();
					return true;
				}
				// if (inner instanceof Expression.MultiByte) {
				// Expression.MultiByte e = (Expression.MultiByte) inner;
				// if (Expression.OneMore) {
				// visit(inner, null);
				// }
				// While(_Match(e.byteseq));
				// {
				// EmptyStatement();
				// }
				// EndWhile();
				// return true;
				// }
				if (inner instanceof PAny) {
					// Expression.Any e = (Expression.Any) inner;
					if (isOneMore) {
						this.visit(inner, null);
					}
					OldParserGenerator.this.While(OldParserGenerator.this._Not(OldParserGenerator.this._Func("eof")));
					{
						OldParserGenerator.this.Statement(OldParserGenerator.this._Func("move", "1"));
					}
					OldParserGenerator.this.EndWhile();
					return true;
				}
			}
			return false;
		}

		private boolean tryAndOptimization(Expression inner) {
			if (OldParserGenerator.this.Optimization) {
				if (inner instanceof PByte) {
					PByte e = (PByte) inner;
					OldParserGenerator.this.If(OldParserGenerator.this._Func("prefetch"), OldParserGenerator.this._NotEq(),
							OldParserGenerator.this._byte(e.byteChar()));
					{
						OldParserGenerator.this.Fail();
					}
					OldParserGenerator.this.EndIf();
					this.checkBinaryEOF(e.byteChar() == 0);
					return true;
				}
				if (inner instanceof PByteSet) {
					PByteSet e = (PByteSet) inner;
					boolean[] byteset = e.bools();
					OldParserGenerator.this.If(OldParserGenerator.this._Not(this.MatchByteArray(byteset, false)));
					{
						OldParserGenerator.this.Fail();
					}
					OldParserGenerator.this.EndIf();
					this.checkBinaryEOF(byteset[0]);
					return true;
				}
				// if (inner instanceof Expression.MultiByte) {
				// Expression.MultiByte e = (Expression.MultiByte) inner;
				// If(_Not(_Match(e.byteseq)));
				// {
				// Fail();
				// }
				// EndIf();
				// return true;
				// }
				if (inner instanceof PAny) {
					// Expression.Any e = (Expression.Any) inner;
					OldParserGenerator.this.If(OldParserGenerator.this._Func("eof"));
					{
						OldParserGenerator.this.Fail();
					}
					OldParserGenerator.this.EndIf();
					return true;
				}
			}
			return false;
		}

		private boolean tryNotOptimization(Expression inner) {
			if (OldParserGenerator.this.Optimization) {
				if (inner instanceof PByte) {
					PByte e = (PByte) inner;
					OldParserGenerator.this.If(OldParserGenerator.this._Func("prefetch"), OldParserGenerator.this._Eq(),
							OldParserGenerator.this._byte(e.byteChar()));
					{
						OldParserGenerator.this.Fail();
					}
					OldParserGenerator.this.EndIf();
					this.checkBinaryEOF(e.byteChar() != 0);
					return true;
				}
				if (inner instanceof PByteSet) {
					PByteSet e = (PByteSet) inner;
					boolean[] byteset = e.bools();
					OldParserGenerator.this.If(this.MatchByteArray(byteset, false));
					{
						OldParserGenerator.this.Fail();
					}
					OldParserGenerator.this.EndIf();
					this.checkBinaryEOF(!byteset[0]);
					return true;
				}
				// if (inner instanceof Expression.MultiByte) {
				// Expression.MultiByte e = (Expression.MultiByte) inner;
				// If(_Match(e.byteseq));
				// {
				// Fail();
				// }
				// EndIf();
				// return true;
				// }
				if (inner instanceof PAny) {
					// Expression.Any e = (Expression.Any) inner;
					OldParserGenerator.this.If(OldParserGenerator.this._Not(OldParserGenerator.this._Func("eof")));
					{
						OldParserGenerator.this.Fail();
					}
					OldParserGenerator.this.EndIf();
					return true;
				}
			}
			return false;
		}

		/* Tree Construction */

		@Override
		public Object visitTree(PTree e, Object a) {
			if (e.folding) {
				OldParserGenerator.this.Statement(OldParserGenerator.this._Func("foldTree",
						OldParserGenerator.this._int(e.beginShift), OldParserGenerator.this._label(e.label)));
			} else {
				OldParserGenerator.this
						.Statement(OldParserGenerator.this._Func("beginTree", OldParserGenerator.this._int(e.beginShift)));
			}
			this.visit(e.get(0), a);
			OldParserGenerator.this.Statement(OldParserGenerator.this._Func("endTree", OldParserGenerator.this._int(e.endShift),
					OldParserGenerator.this._tag(e.tag), OldParserGenerator.this._text(e.value)));
			return null;
		}

		@Override
		public Object visitLinkTree(PLinkTree e, Object a) {
			this.BeginScope();
			String tree = this.SaveTree();
			this.visit(e.get(0), a);
			OldParserGenerator.this.Statement(
					OldParserGenerator.this._Func("linkTree", /* _Null(), */OldParserGenerator.this._label(e.label)));
			this.BackTree(tree);
			this.EndScope();
			return null;
		}

		@Override
		public Object visitTag(PTag e, Object a) {
			OldParserGenerator.this.Statement(OldParserGenerator.this._Func("tagTree", OldParserGenerator.this._tag(e.tag)));
			return null;
		}

		@Override
		public Object visitReplace(PValue e, Object a) {
			OldParserGenerator.this
					.Statement(OldParserGenerator.this._Func("valueTree", OldParserGenerator.this._text(e.value)));
			return null;
		}

		@Override
		public Object visitDetree(PDetree e, Object a) {
			this.BeginScope();
			String n1 = this.SaveTree();
			String n2 = this.SaveLog();
			this.visit(e.get(0), a);
			this.BackTree(n1);
			this.BackLog(n2);
			this.EndScope();
			return null;
		}

		@Override
		public Object visitSymbolScope(PSymbolScope e, Object a) {
			this.BeginScope();
			String n = this.SaveSymbolTable();
			if (e.label != null) {
				OldParserGenerator.this
						.Statement(OldParserGenerator.this._Func("addSymbolMask", OldParserGenerator.this._table(e.label)));
			}
			this.visit(e.get(0), a);
			this.BackSymbolTable(n);
			this.EndScope();
			return null;
		}

		@Override
		public Object visitSymbolAction(PSymbolAction e, Object a) {
			// BeginScope();
			// String ppos = SavePos();
			// visit(e.get(0), a);
			// Statement(_Func("addSymbol", _table(e.param), ppos));
			// EndScope();
			return null;
		}

		@Override
		public Object visitSymbolPredicate(PSymbolPredicate e, Object a) {
			// BeginScope();
			// String ppos = SavePos();
			// visit(e.get(0), a);
			// if (e.funcName == NezFunc.is) {
			// If(_Not(_Func("equals", _table(e.tableName), ppos)));
			// {
			// Fail();
			// }
			// EndIf();
			// } else {
			// If(_Not(_Func("contains", _table(e.tableName), ppos)));
			// {
			// Fail();
			// }
			// EndIf();
			// }
			// EndScope();
			return null;
		}

		// @Override
		// public Object visitSymbolMatch(Expression.SymbolMatch e, Object a) {
		// If(_Not(_Func("matchSymbol", _table(e.tableName))));
		// {
		// Fail();
		// }
		// EndIf();
		// return null;
		// }
		//
		// @Override
		// public Object visitSymbolExists(Expression.SymbolExists e, Object a)
		// {
		// if (e.symbol == null) {
		// If(_Not(_Func("exists", _table(e.tableName))));
		// {
		// Fail();
		// }
		// EndIf();
		// } else {
		// If(_Not(_Func("existsSymbol", _table(e.tableName),
		// _text(e.symbol))));
		// {
		// Fail();
		// }
		// EndIf();
		// }
		// return null;
		// }

		@Override
		public Object visitScan(PScan e, Object a) {
			this.BeginScope();
			String ppos = this.SavePos();
			this.visit(e.get(0), a);
			OldParserGenerator.this.Statement(OldParserGenerator.this._Func("scanCount", ppos,
					OldParserGenerator.this._long(e.mask), OldParserGenerator.this._int(e.shift)));
			this.EndScope();
			return null;
		}

		@Override
		public Object visitRepeat(PRepeat e, Object a) {
			OldParserGenerator.this.While(OldParserGenerator.this._Func("decCount"));
			{
				this.visit(e.get(0), a);
			}
			OldParserGenerator.this.EndWhile();
			return null;
		}

		@Override
		public Object visitIf(PIfCondition e, Object a) {
			return null;
		}

		@Override
		public Object visitOn(POnCondition e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitTrap(PTrap e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	/* Syntax */

	// @Override
	// protected String _LineComment() {
	// return "//";
	// }

	protected String _Comment(String c) {
		return "/*" + c + "*/";
	}

	protected String _And() {
		return "&&";
	}

	protected String _Or() {
		return "||";
	}

	protected String _Not(String expr) {
		return "!" + expr;
	}

	protected String _Eq() {
		return "==";
	}

	protected String _NotEq() {
		return "!=";
	}

	protected String _True() {
		return "true";
	}

	protected String _False() {
		return "false";
	}

	protected String _Null() {
		return "null";
	}

	/* Expression */

	private String _GetArray(String array, String c) {
		return array + "[" + c + "]";
	}

	protected String _BeginArray() {
		return "{";
	}

	protected String _EndArray() {
		return "}";
	}

	protected String _BeginBlock() {
		return " {";
	}

	protected String _EndBlock() {
		return "}";
	}

	protected String _Field(String o, String name) {
		return o + "." + name;
	}

	protected String _Func(String name, String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append(this._state());
		sb.append(".");
		sb.append(name);
		sb.append("(");
		for (int i = 0; i < args.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(args[i]);
		}
		sb.append(")");
		return sb.toString();
	}

	protected String _byte(int ch) {
		// if (ch < 128 && (!Character.isISOControl(ch))) {
		// return "'" + (char) ch + "'";
		// }
		return "" + (ch & 0xff);
	}

	protected String _Match(byte[] t) {
		if (this.SupportedMatch2 && this.isMatchText(t, 2)) {
			return this._Func("match2", this._byte(t[0]), this._byte(t[1]));
		}
		if (this.SupportedMatch3 && this.isMatchText(t, 3)) {
			return this._Func("match3", this._byte(t[0]), this._byte(t[1]), this._byte(t[2]));
		}
		if (this.SupportedMatch4 && this.isMatchText(t, 4)) {
			return this._Func("match4", this._byte(t[0]), this._byte(t[1]), this._byte(t[2]), this._byte(t[3]));
		}
		if (this.SupportedMatch4 && this.isMatchText(t, 5)) {
			return this._Func("match5", this._byte(t[0]), this._byte(t[1]), this._byte(t[2]), this._byte(t[3]),
					this._byte(t[4]));
		}
		if (this.SupportedMatch4 && this.isMatchText(t, 6)) {
			return this._Func("match6", this._byte(t[0]), this._byte(t[1]), this._byte(t[2]), this._byte(t[3]),
					this._byte(t[4]), this._byte(t[5]));
		}
		if (this.SupportedMatch4 && this.isMatchText(t, 7)) {
			return this._Func("match7", this._byte(t[0]), this._byte(t[1]), this._byte(t[2]), this._byte(t[3]),
					this._byte(t[4]), this._byte(t[5]), this._byte(t[6]));
		}
		if (this.SupportedMatch4 && this.isMatchText(t, 8)) {
			return this._Func("match8", this._byte(t[0]), this._byte(t[1]), this._byte(t[2]), this._byte(t[3]),
					this._byte(t[4]), this._byte(t[5]), this._byte(t[6]), this._byte(t[7]));
		}
		return this._Func("match", this._text(t));
	}

	protected String _int(int n) {
		return "" + n;
	}

	protected String _hex(int n) {
		return String.format("0x%08x", n);
	}

	protected String _long(long n) {
		return "" + n + "L";
	}

	/* Expression */

	protected String _defun(String type, String name) {
		return "private static <T> " + type + "name";
	}

	protected String _argument(String var, String type) {
		if (type == null) {
			return var;
		}
		return type + " " + var;
	}

	protected String _argument() {
		return this._argument(this._state(), this.type(this._state()));
	}

	protected String _funccall(String name) {
		return name + "(" + this._state() + ")";
	}

	/* Statement */

	protected void BeginDecl(String line) {
		this.L(line);
		this.pBegin();
	}

	protected void EndDecl() {
		this.pEnd();
	}

	protected void BeginFunc(String type, String name, String args) {
		this.L(this._defun(type, name));
		this.p("(");
		this.p(args);
		this.p(")");
		this.pBegin();
	}

	protected final void BeginFunc(String f, String args) {
		this.BeginFunc(this.type("$parse"), f, args);
	}

	protected final void BeginFunc(String f) {
		this.BeginFunc(this.type("$parse"), f, this._argument());
	}

	protected void EndFunc() {
		this.pEnd();
	}

	protected void BeginLocalScope() {
		this.L("{");
		this.incIndent();
	}

	protected void EndLocalScope() {
		this.decIndent();
		this.L("}");
	}

	protected void Statement(String stmt) {
		this.L(stmt);
		this._Semicolon();
	}

	protected void EmptyStatement() {
		this.L("");
		this._Semicolon();
	}

	protected void _Semicolon() {
		this.p(";");
	}

	protected void Return(String expr) {
		this.Statement("return " + expr);
	}

	protected void Succ() {
		this.Return(this._True());
	}

	protected void Fail() {
		this.Return(this._False());
	}

	protected void If(String cond) {
		this.L("if (");
		this.p(cond);
		this.p(")");
		this.pBegin();
	}

	protected String _Binary(String a, String op, String b) {
		return a + " " + op + " " + b;
	}

	protected void If(String a, String op, String b) {
		this.If(a + " " + op + " " + b);
	}

	protected void Else() {
		this.pEnd();
		this.p(" else");
		this.pBegin();
	}

	protected void EndIf() {
		this.pEnd();
	}

	protected void While(String cond) {
		this.L("while (");
		this.p(cond);
		this.p(")");
		this.pBegin();
	}

	protected void EndWhile() {
		this.pEnd();
	}

	protected void Do() {
		this.p("do");
		this.pBegin();
	}

	protected void DoWhile(String cond) {
		this.pEnd();
		this.p("while (");
		this.p(cond);
		this.p(")");
		this._Semicolon();
	}

	protected void Break() {
		this.L("break");
		this._Semicolon();
	}

	protected void Switch(String c) {
		this.L("switch(" + c + ")");
		this.pBegin();
	}

	protected void EndSwitch() {
		this.pEnd();
	}

	protected void Case(String n) {
		this.L("case " + n + ": ");
	}

	protected void EndCase() {
	}

	protected void VarDecl(String name, String expr) {
		this.VarDecl(this.type(name), name, expr);
	}

	protected void VarDecl(String type, String name, String expr) {
		if (name == null) {
			this.VarAssign(name, expr);
		} else {
			this.Statement(type + " " + name + " = " + expr);
		}
	}

	protected void VarAssign(String v, String expr) {
		this.Statement(v + " = " + expr);
	}

	protected void DeclConst(String type, String name, String val) {
		if (type == null) {
			this.Statement("private final static " + name + " = " + val);
		} else {
			this.Statement("private final static " + type + " " + name + " = " + val);
		}
	}

	protected String _arity(int arity) {
		return "[" + arity + "]";
	}

	protected void DeclConst(String type, String name, int arity, String val) {
		if (this.type("$arity") != null) {
			this.DeclConst(type, name + this._arity(arity), val);
		} else {
			this.DeclConst(type, name, val);
		}
	}

	protected void GCinc(String expr) {
	}

	protected void GCdec(String expr) {
	}

	/* Variables */

	protected String _state() {
		return "c";
	}

	protected String _pos() {
		return "pos";
	}

	protected String _cpos() {
		return this._Field(this._state(), "pos");
	}

	protected String _tree() {
		return "left";
	}

	protected String _log() {
		return "log";
	}

	protected String _table() {
		return "sym";
	}

	protected String _temp() {
		return "temp";
	}

	protected String _index() {
		return "_index";
	}

	protected String _set() {
		return "_set";
	}

	protected String _range() {
		return "_range";
	}

	protected String _text() {
		return "_text";
	}

	// protected String _arity(String name) {
	// return name + "_len";
	// }

	protected void InitMemoPoint() {
		if (this.code.getMemoPointSize() > 0) {
			this.Statement(this._Func("initMemo",
					"64/*FIXME*/"/*
									 * _int(strategy.SlidingWindow )
									 */, this._int(this.code.getMemoPointSize())));
		}
	}
}
