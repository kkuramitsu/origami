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

package origami.main.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import origami.main.CommonWriter;
import origami.main.OOption;
import origami.main.ParserOption;
import origami.nez.ast.Symbol;
import origami.nez.parser.Parser;
import origami.nez.parser.ParserCode;
import origami.nez.parser.ParserCode.MemoPoint;

import origami.nez.peg.Expression;
import origami.nez.peg.ExpressionVisitor;
import origami.nez.peg.NezFunc;
import origami.nez.peg.NonEmpty;
import origami.nez.peg.Grammar;
import origami.nez.peg.Production;
import origami.nez.peg.Typestate;
import origami.util.ODebug;
import origami.util.OStringUtils;
import origami.util.OVerbose;

public abstract class ParserGenerator
		extends CommonWriter /* implements GrammarWriter */ {

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
			fileBase = extractGrammarName((String) options.get(ParserOption.GrammarFile));
			Parser parser = g.newParser(options);
			this.code = (ParserCode<?>) parser.compile();
			this.initLanguageSpec();
			this.generateHeader(g);
			SymbolAnalysis constDecl = new SymbolAnalysis();
			constDecl.decl(g.getStartProduction());
			this.sortFuncList(_funcname(g.getStartProduction()));
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
		return _funcname(p.getUniqueName());
	}

	protected String _funcname(String uname) {
		return "p" + _rename(uname);
	}

	protected String _rename(String name) {
		return name.replace("!", "NOT").replace("~", "_").replace("&", "AND");
	}

	/* Types */

	protected HashMap<String, String> typeMap = new HashMap<>();

	protected abstract void initLanguageSpec();

	protected void addType(String name, String type) {
		typeMap.put(name, type);
	}

	protected String type(String name) {
		return typeMap.get(name);
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
		String v = nameMap.get(key);
		return v;
	}

	final String _range(boolean[] b) {
		String key = OStringUtils.stringfyBitmap(b) + "*";
		String v = nameMap.get(key);
		return v;
	}

	final void DeclSet(boolean[] b, boolean Iteration) {
		if (Iteration && SSEOption) {
			byte[] range = rangeSEE(b);
			if (range != null) {
				String key = OStringUtils.stringfyBitmap(b) + "*";
				String name = nameMap.get(key);
				if (name == null) {
					name = _range() + nameMap.size();
					nameMap.put(key, name);
					DeclConst(type("$range"), name, range.length, _initByteArray(range));
				}
				return;
			}
		}
		if (this.SupportedRange && range(b) != null) {
			return;
		}
		String key = OStringUtils.stringfyBitmap(b);
		String name = nameMap.get(key);
		if (name == null) {
			name = _set() + nameMap.size();
			nameMap.put(key, name);
			DeclConst(type("$set"), name, UsingBitmap ? 8 : 256, _initBooleanArray(b));
		}
	}

	final String _index(byte[] b) {
		String key = key(b);
		return nameMap.get(key);
	}

	final void DeclIndex(byte[] b) {
		String key = key(b);
		String name = nameMap.get(key);
		if (name == null) {
			name = _index() + nameMap.size();
			nameMap.put(key, name);
			DeclConst(type("$index"), name, b.length, _initByteArray(b));
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
		return nameMap.get(key);
	}

	protected String _text(String key) {
		if (key == null) {
			return _Null();
		}
		return nameMap.get(key);
	}

	final void DeclText(byte[] text) {
		String key = new String(text);
		String name = nameMap.get(key);
		if (name == null) {
			name = _text() + nameMap.size();
			nameMap.put(key, name);
			DeclConst(type("$text"), name, text.length, _initByteArray(text));
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
		if ((SupportedMatch2 && isMatchText(text, 2)) || (SupportedMatch3 && isMatchText(text, 3))
				|| (SupportedMatch4 && isMatchText(text, 4)) || (SupportedMatch5 && isMatchText(text, 5))
				|| (SupportedMatch6 && isMatchText(text, 6)) || (SupportedMatch7 && isMatchText(text, 7))
				|| (SupportedMatch8 && isMatchText(text, 8))) {
			return;
		}
		DeclText(text);
	}

	final String _tag(Symbol s) {
		if (!this.UniqueNumberingSymbol && s == null) {
			return _Null();
		}
		return _tagname(s == null ? "" : s.getSymbol());
	}

	final void DeclTag(String s) {
		if (!tagMap.containsKey(s)) {
			int n = tagMap.size();
			tagMap.put(s, n);
			tagList.add(s);
			DeclConst(this.type("$tag"), _tagname(s), _initTag(n, s));
		}
	}

	final String _label(Symbol s) {
		if (!this.UniqueNumberingSymbol && s == null) {
			return _Null();
		}
		return _labelname(s == null ? "" : s.getSymbol());
	}

	final void DeclTrap(String s) {
		if (!labelMap.containsKey(s)) {
			int n = labelMap.size();
			labelMap.put(s, n);
			labelList.add(s);
			if (this.UniqueNumberingSymbol || !s.equals("_")) {
				DeclConst(type("$label"), _labelname(s), _initTrap(n, s));
			}
		}
	}

	final String _table(Symbol s) {
		if (!this.UniqueNumberingSymbol && s.equals("")) {
			return _Null();
		}
		return _tablename(s == null ? "" : s.getSymbol());
	}

	final void DeclTable(Symbol t) {
		String s = t.getSymbol();
		if (!tableMap.containsKey(s)) {
			int n = tableMap.size();
			tableMap.put(s, n);
			tableList.add(s);
			DeclConst(type("$table"), _tablename(s), _initTable(n, s));
		}
	}

	final void generateSymbolTables() {
		if (UniqueNumberingSymbol) {
			generateSymbolTable("_tags", tagList);
			generateSymbolTable("_labels", labelList);
			generateSymbolTable("_tables", tableList);
		}
	}

	private void generateSymbolTable(String name, ArrayList<String> l) {
		if (l.size() > 0) {
			DeclConst(this.type("$string"), name, l.size(), _initStringArray(l, l.size()));
		}
	}

	String fileBase;

	protected String _basename() {
		return fileBase;
	}

	protected String _ns() {
		return fileBase + "_";
	}

	protected String _quote(String s) {
		if (s == null) {
			return "\"\"";
		}
		return OStringUtils.quoteString('"', s.toString(), '"');
	}

	protected String _initBooleanArray(boolean[] b) {
		StringBuilder sb = new StringBuilder();
		sb.append(_BeginArray());
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
				sb.append(_True());
			} else {
				sb.append(_False());
			}
		}
		// }
		sb.append(_EndArray());
		return sb.toString();
	}

	protected String _initByteArray(byte[] b) {
		StringBuilder sb = new StringBuilder();
		sb.append(_BeginArray());
		for (int i = 0; i < b.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			// sb.append(_int(b[i] & 0xff));
			sb.append(b[i]);
		}
		sb.append(_EndArray());
		return sb.toString();
	}

	protected String _initStringArray(List<String> a, int size) {
		StringBuilder sb = new StringBuilder();
		sb.append(_BeginArray());
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(_quote(a.get(i)));
		}
		sb.append(_EndArray());
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
		return UniqueNumberingSymbol ? "" + id : _quote(s);
	}

	protected String _initTrap(int id, String s) {
		return UniqueNumberingSymbol ? "" + id : _quote(s);
	}

	protected String _initTable(int id, String s) {
		return UniqueNumberingSymbol ? "" + id : _quote(s);
	}

	/* function */
	HashMap<String, String> exprMap = new HashMap<>();
	HashMap<String, Expression> funcMap = new HashMap<>();
	ArrayList<String> funcList = new ArrayList<>();
	HashSet<String> crossRefNames = new HashSet<>();
	HashMap<String, Integer> memoPointMap = new HashMap<>();

	private String _funcname(Expression e) {
		if (e instanceof Expression.PNonTerminal) {
			return _funcname(((Expression.PNonTerminal) e).getUniqueName());
		}
		String key = e.toString();
		return exprMap.get(key);
	}

	HashMap<String, HashSet<String>> nodes = new HashMap<>();

	private void addEdge(String sour, String dest) {
		if (sour != null) {
			HashSet<String> set = nodes.get(sour);
			if (set == null) {
				set = new HashSet<>();
				nodes.put(sour, set);
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
						visit(e.getKey(), e.getValue());
					}
				}
			}

			private void visit(String key, HashSet<String> nextNodes) {
				visited.put(key, Visiting);
				if (nextNodes != null) {
					for (String nextNode : nextNodes) {
						Short v = this.visited.get(nextNode);
						if (v == null) {
							visit(nextNode, nodes.get(nextNode));
						} else if (v == Visiting) {
							if (!key.equals(nextNode)) {
								// Verbose.println("Cyclic " + key + " => " +
								// nextNode);
								crossRefNames.add(nextNode);
							}
						}
					}
				}
				visited.put(key, Visited);
				result.add(key);
			}

			public ArrayList<String> getResult() {
				return new ArrayList<>(result);
			}
		}
		TopologicalSorter sorter = new TopologicalSorter(nodes);
		funcList = sorter.getResult();
		if (!funcList.contains(start)) {
			funcList.add(start);
		}
		nodes.clear();
	}

	private class SymbolAnalysis extends ExpressionVisitor<Object, Object> {

		SymbolAnalysis() {
			DeclTag("");
			DeclTrap("");
			DeclTable(Symbol.Null);
		}

		private Object decl(Production p) {
			if (checkFuncName(p)) {
				p.getExpression().visit(this, null);
			}
			return null;
		}

		String cur = null; // name

		private boolean checkFuncName(Production p) {
			String f = _funcname(p.getUniqueName());
			if (!funcMap.containsKey(f)) {
				String stacked2 = cur;
				cur = f;
				funcMap.put(f, p.getExpression());
				funcList.add(f);
				MemoPoint memoPoint = code.getMemoPoint(p.getUniqueName());
				if (memoPoint != null) {
					memoPointMap.put(f, memoPoint.id);
					String stacked = cur;
					cur = f;
					checkInner(p.getExpression());
					cur = stacked;
					addEdge(cur, f);
				} else {
					p.getExpression().visit(this, null);
				}
				cur = stacked2;
				addEdge(cur, f);
				return true;
			}
			addEdge(cur, f);
			// crossRefNames.add(f);
			return false;
		}

		private void checkInner(Expression e) {
			if (e instanceof Expression.PNonTerminal) {
				e.visit(this, null);
				return;
			}
			String key = e.toString();
			String f = exprMap.get(key);
			if (f == null) {
				f = "e" + exprMap.size();
				exprMap.put(key, f);
				funcList.add(f);
				funcMap.put(f, e);
				String stacked = cur;
				cur = f;
				e.visit(this, null);
				cur = stacked;
			}
			addEdge(cur, f);
			// crossRefNames.add(f);
		}

		private void checkNonLexicalInner(Expression e) {
			if (Optimization) {
				if (e instanceof Expression.PByte || e instanceof Expression.PByteSet || e instanceof Expression.PAny) {
					e.visit(this, null);
					return;
				}
			}
			checkInner(e);
		}

		private Object visitInnerAll(Expression e) {
			for (Expression sub : e) {
				sub.visit(this, null);
			}
			return null;
		}

		@Override
		public Object visitNonTerminal(Expression.PNonTerminal e, Object a) {
			return decl(e.getProduction());
		}

		@Override
		public Object visitEmpty(Expression.PEmpty e, Object a) {
			return null;
		}

		@Override
		public Object visitFail(Expression.PFail e, Object a) {
			return null;
		}

		@Override
		public Object visitByte(Expression.PByte e, Object a) {
			return null;
		}

		@Override
		public Object visitByteSet(Expression.PByteSet e, Object a) {
			DeclSet(e.byteSet(), false);
			return null;
		}

		@Override
		public Object visitAny(Expression.PAny e, Object a) {
			return null;
		}

		// @Override
		// public Object visitMultiByte(Expression.MultiByte e, Object a) {
		// DeclMatchText(e.byteseq);
		// return null;
		// }

		@Override
		public Object visitPair(Expression.PPair e, Object a) {
			return visitInnerAll(e);
		}

		@Override
		public Object visitChoice(Expression.PChoice e, Object a) {
			// if (e.predicted != null) {
			// DeclIndex(e.predicted.indexMap);
			// }
			if (e.size() == 1) {
				// single selection
				e.get(0).visit(this, null);
			} else {
				for (Expression sub : e) {
					checkInner(sub);
				}
			}
			return null;
		}

		@Override
		public Object visitDispatch(Expression.PDispatch e, Object a) {
			DeclIndex(e.indexMap);
			for (Expression sub : e) {
				checkInner(sub);
			}
			return null;
		}

		@Override
		public Object visitOption(Expression.POption e, Object a) {
			checkNonLexicalInner(e.get(0));
			return null;
		}

		@Override
		public Object visitRepetition(Expression.PRepetition e, Object a) {
			if (Optimization && e.get(0) instanceof Expression.PByteSet) {
				DeclSet(((Expression.PByteSet) e.get(0)).byteSet(), true);
				return null;
			}
			checkNonLexicalInner(e.get(0));
			return null;
		}

		// @Override
		// public Object visitOneMore(Expression.OneMore e, Object a) {
		// if (Optimization && e.get(0) instanceof Expression.ByteSet) {
		// DeclSet(((Expression.ByteSet) e.get(0)).byteset(), true);
		// return null;
		// }
		// checkNonLexicalInner(e.get(0));
		// return null;
		// }

		@Override
		public Object visitAnd(Expression.PAnd e, Object a) {
			checkNonLexicalInner(e.get(0));
			return null;
		}

		@Override
		public Object visitNot(Expression.PNot e, Object a) {
			checkNonLexicalInner(e.get(0));
			return null;
		}

		// @Override
		// public Object visitBeginTree(Expression.BeginTree e, Object a) {
		// return null;
		// }

		@Override
		public Object visitTree(Expression.PTree e, Object a) {
			if (e.label != null) {
				DeclTrap(e.label.getSymbol());
			}
			if (e.tag != null) {
				DeclTag(e.tag.getSymbol());
			}
			if (e.value != null) {
				DeclText(OStringUtils.utf8(e.value));
			}
			return visitInnerAll(e);
		}

		@Override
		public Object visitLinkTree(Expression.PLinkTree e, Object a) {
			if (e.label != null) {
				DeclTrap(e.label.getSymbol());
			}
			return visitInnerAll(e);
		}

		@Override
		public Object visitTag(Expression.PTag e, Object a) {
			DeclTag(e.tag.getSymbol());
			return null;
		}

		@Override
		public Object visitReplace(Expression.PReplace e, Object a) {
			DeclText(OStringUtils.utf8(e.value));
			return null;
		}

		@Override
		public Object visitDetree(Expression.PDetree e, Object a) {
			return visitInnerAll(e);
		}

		@Override
		public Object visitSymbolScope(Expression.PSymbolScope e, Object a) {
			return visitInnerAll(e);
		}

		@Override
		public Object visitSymbolAction(Expression.PSymbolAction e, Object a) {
			DeclTable(e.table);
			return visitInnerAll(e);
		}

		@Override
		public Object visitSymbolPredicate(Expression.PSymbolPredicate e, Object a) {
			DeclTable(e.table);
			return visitInnerAll(e);
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
		public Object visitScan(Expression.PScan e, Object a) {
			return visitInnerAll(e);
		}

		@Override
		public Object visitRepeat(Expression.PRepeat e, Object a) {
			return visitInnerAll(e);
		}

		@Override
		public Object visitIf(Expression.PIfCondition e, Object a) {
			return null;
		}

		@Override
		public Object visitOn(Expression.POnCondition e, Object a) {
			return visitInnerAll(e);
		}

		@Override
		public Object visitTrap(Expression.PTrap e, Object a) {
			return null;
		}
	}

	class ParserGeneratorVisitor extends ExpressionVisitor<Object, Object> {

		void generate() {
			for (String f : funcList) {
				generateFunction(f, funcMap.get(f));
			}
		}

		private String _eval(Expression e) {
			String f = _funcname(e);
			if (f == null) {
				return null;
			}
			return _funccall(f);
		}

		private String _eval(String uname) {
			return _funccall(_funcname(uname));
		}

		private void generateFunction(String name, Expression e) {
			Integer memoPoint = memoPointMap.get(name);
			Verbose(e.toString());
			initLocal();
			BeginFunc(name);
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
					InitVal("memo", _Func(memoLookup, _int(memoPoint)));
					If("memo", _Eq(), "0");
					{
						String f = _eval(e);
						String[] n = SaveState(e);
						If(f);
						{
							Statement(_Func(memoSucc, _int(memoPoint), n[0]));
							Succ();
						}
						Else();
						{
							BackState(e, n);
							Statement(_Func(memoFail, _int(memoPoint)));
							Fail();
						}
						EndIf();
					}
					EndIf();
					Return(_Binary("memo", _Eq(), "1"));
				} else {
					visit(e, null);
					Succ();
				}
			}
			EndFunc();
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
			if (nested > 0) {
				BeginLocalScope();
			}
		}

		protected void EndScope() {
			if (nested > 0) {
				EndLocalScope();
			}
		}

		HashMap<String, String> localMap;

		private void initLocal() {
			localMap = new HashMap<>();
		}

		private String local(String name) {
			if (!localMap.containsKey(name)) {
				localMap.put(name, name);
				return name;
			}
			return local(name + localMap.size());
		}

		private String InitVal(String name, String expr) {
			String type = type(name);
			String lname = local(name);
			VarDecl(type, lname, expr);
			return lname;
		}

		private String SavePos() {
			return InitVal(_pos(), _Field(_state(), "pos"));
		}

		private void BackPos(String lname) {
			VarAssign(_Field(_state(), "pos"), lname);
		}

		private String SaveTree() {
			return InitVal(_tree(), _Func("saveTree"));
		}

		private void BackTree(String lname) {
			Statement(_Func("backTree", lname));
		}

		private String SaveLog() {
			return InitVal(_log(), _Func("saveLog"));
		}

		private void BackLog(String lname) {
			Statement(_Func("backLog", lname));
		}

		private String SaveSymbolTable() {
			return InitVal(_table(), _Func("saveSymbolPoint"));
		}

		private void BackSymbolTable(String lname) {
			Statement(_Func("backSymbolPoint", lname));
		}

		private String[] SaveState(Expression inner) {
			String[] names = new String[4];
			names[0] = SavePos();
			if (Typestate.compute(inner) != Typestate.Unit) {
				names[1] = SaveTree();
				names[2] = SaveLog();
			}
			// FIXME: To Kayaban and Omega Celery
			// if (symbolMutation.isMutated(inner)) {
			// names[3] = SaveSymbolTable();
			// }
			return names;
		}

		private void BackState(Expression inner, String[] names) {
			BackPos(names[0]);
			if (names[1] != null) {
				BackTree(names[1]);
			}
			if (names[2] != null) {
				BackLog(names[2]);
			}
			if (names[3] != null) {
				BackSymbolTable(names[3]);
			}
		}

		@Override
		public Object visitNonTerminal(Expression.PNonTerminal e, Object a) {
			String f = _eval(e.getUniqueName());
			If(_Not(f));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitEmpty(Expression.PEmpty e, Object a) {
			return null;
		}

		@Override
		public Object visitFail(Expression.PFail e, Object a) {
			Fail();
			return null;
		}

		@Override
		public Object visitByte(Expression.PByte e, Object a) {
			If(_Func("read"), _NotEq(), _byte(e.byteChar));
			{
				Fail();
			}
			EndIf();
			checkBinaryEOF(e.byteChar == 0);
			return null;
		}

		@Override
		public Object visitByteSet(Expression.PByteSet e, Object a) {
			boolean[] byteset = e.byteSet();
			If(_Not(MatchByteArray(byteset, true)));
			{
				Fail();
			}
			EndIf();
			checkBinaryEOF(byteset[0]);
			return null;
		}

		private void checkBinaryEOF(boolean checked) {
			if (BinaryGrammar && checked) {
				If(_Func("eof"));
				{
					Fail();
				}
				EndIf();
			}
		}

		private String MatchByteArray(boolean[] byteMap, boolean inc) {
			String c = inc ? _Func("read") : _Func("prefetch");
			if (SupportedRange) {
				int[] range = range(byteMap);
				if (range != null) {
					String s = "(" + _Binary(_Binary(_byte(range[0]), "<=", _Func("prefetch")), _And(),
							_Binary(c, "<", _byte(range[1]))) + ")";
					// System.out.println(s);
					return s;
				}
			}
			if (UsingBitmap) {
				return _Func("bitis", _set(byteMap), c);
			} else {
				return _GetArray(_set(byteMap), c);
			}
		}

		@Override
		public Object visitAny(Expression.PAny e, Object a) {
			if (BinaryGrammar) {
				Statement(_Func("move", "1"));
				If(_Func("eof"));
				{
					Fail();
				}
				EndIf();
			} else {
				If(_Func("read"), _Eq(), "0");
				{
					Fail();
				}
				EndIf();
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
		public Object visitPair(Expression.PPair e, Object a) {
			for (Expression sub : e) {
				visit(sub, a);
			}
			return null;
		}

		@Override
		public Object visitChoice(Expression.PChoice e, Object a) {
			// if (e.predicted != null && SupportedSwitchCase) {
			// generateSwitch(e, e.predicted);
			// } else {
			BeginScope();
			String temp = InitVal(_temp(), _True());
			for (Expression sub : e) {
				String f = _eval(sub);
				If(temp);
				{
					String[] n = SaveState(sub);
					Verbose(sub.toString());
					If(f);
					{
						VarAssign(temp, _False());
					}
					Else();
					{
						BackState(sub, n);
					}
					EndIf();
				}
				EndIf();
			}
			If(temp);
			{
				Fail();
			}
			EndIf();
			EndScope();
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
		public Object visitDispatch(Expression.PDispatch e, Object a) {
			String temp = InitVal(_temp(), _True());
			Switch(_GetArray(_index(e.indexMap), _Func("prefetch")));
			Case("0");
			Fail();
			int caseIndex = 1;
			for (Expression sub : e) {
				Case(_int(caseIndex));
				String f = _eval(sub);
				Verbose(sub.toString());
				VarAssign(temp, f);
				Break();
				EndCase();
				caseIndex++;
			}
			EndSwitch();
			If(_Not(temp));
			{
				Fail();
			}
			EndIf();
			return null;
		}

		@Override
		public Object visitOption(Expression.POption e, Object a) {
			Expression sub = e.get(0);
			if (!tryOptionOptimization(sub)) {
				String f = _eval(sub);
				String[] n = SaveState(sub);
				Verbose(sub.toString());
				If(_Not(f));
				{
					BackState(sub, n);
				}
				EndIf();
			}
			return null;
		}

		@Override
		public Object visitRepetition(Expression.PRepetition e, Object a) {
			if (e.isOneMore()) {
				if (!this.tryRepetitionOptimization(e.get(0), true)) {
					String f = _eval(e.get(0));
					if (f != null) {
						If(_Not(f));
						{
							Fail();
						}
						EndIf();
					} else {
						visit(e.get(0), a);
					}
					generateWhile(e, a);
				}
			} else {
				if (!this.tryRepetitionOptimization(e.get(0), false)) {
					generateWhile(e, a);
				}
			}
			return null;
		}

		private void generateWhile(Expression e, Object a) {
			Expression sub = e.get(0);
			String f = _eval(sub);
			While(_True());
			{
				String[] n = SaveState(sub);
				Verbose(sub.toString());
				If(_Not(f));
				{
					BackState(sub, n);
					Break();
				}
				EndIf();
				CheckInfiniteLoop(sub, n[0]);
			}
			EndWhile();
		}

		private void CheckInfiniteLoop(Expression e, String var) {
			if (!NonEmpty.isAlwaysConsumed(e)) {
				If(var, _Eq(), _Field(_state(), "pos"));
				{
					Break();
				}
				EndIf();
			}
		}

		@Override
		public Object visitAnd(Expression.PAnd e, Object a) {
			Expression sub = e.get(0);
			if (!this.tryAndOptimization(sub)) {
				String f = _eval(sub);
				BeginScope();
				String n = SavePos();
				Verbose(sub.toString());
				If(_Not(f));
				{
					Fail();
				}
				EndIf();
				BackPos(n);
				EndScope();
			}
			return null;
		}

		@Override
		public Object visitNot(Expression.PNot e, Object a) {
			Expression sub = e.get(0);
			if (!this.tryNotOptimization(sub)) {
				String f = _eval(sub);
				BeginScope();
				String[] n = SaveState(sub);
				Verbose(sub.toString());
				If(f);
				{
					Fail();
				}
				EndIf();
				BackState(sub, n);
				EndScope();
			}
			return null;
		}

		private boolean tryOptionOptimization(Expression inner) {
			if (Optimization) {
				if (inner instanceof Expression.PByte) {
					Expression.PByte e = (Expression.PByte) inner;
					If(_Func("prefetch"), _Eq(), _byte(e.byteChar));
					{
						if (BinaryGrammar && e.byteChar == 0) {
							If(_Not(_Func("eof")));
							{
								Statement(_Func("move", "1"));
							}
							EndIf();
						} else {
							Statement(_Func("move", "1"));
						}
					}
					EndIf();
					return true;
				}
				if (inner instanceof Expression.PByteSet) {
					Expression.PByteSet e = (Expression.PByteSet) inner;
					boolean[] byteset = e.byteSet();
					If(MatchByteArray(byteset, false));
					{
						if (BinaryGrammar && byteset[0]) {
							If(_Not(_Func("eof")));
							{
								Statement(_Func("move", "1"));
							}
							EndIf();
						} else {
							Statement(_Func("move", "1"));
						}
					}
					EndIf();
					return true;
				}
				// if (inner instanceof Expression.MultiByte) {
				// Expression.MultiByte e = (Expression.MultiByte) inner;
				// Statement(_Match(e.byteseq));
				// return true;
				// }
				if (inner instanceof Expression.PAny) {
					// Expression.Any e = (Expression.Any) inner;
					If(_Not(_Func("eof")));
					{
						Statement(_Func("move", "1"));
					}
					EndIf();
					return true;
				}
			}
			return false;
		}

		private boolean tryRepetitionOptimization(Expression inner, boolean isOneMore) {
			if (Optimization) {
				if (inner instanceof Expression.PByte) {
					Expression.PByte e = (Expression.PByte) inner;
					if (isOneMore) {
						visit(inner, null);
					}
					While(_Binary(_Func("prefetch"), _Eq(), _byte(e.byteChar)));
					{
						if (BinaryGrammar && e.byteChar == 0) {
							If(_Func("eof"));
							{
								Break();
							}
							EndIf();
						}
						Statement(_Func("move", "1"));
					}
					EndWhile();
					return true;
				}
				if (inner instanceof Expression.PByteSet) {
					Expression.PByteSet e = (Expression.PByteSet) inner;
					boolean[] byteset = e.byteSet();
					if (SSEOption) {
						String name = _range(byteset);
						if (name != null) {
							byte[] r = rangeSEE(byteset);
							OVerbose.println("range: " + name + " " + e);
							if (isOneMore) {
								If(_Not(_Func("checkOneMoreRange", name, _int(r.length))));
								{
									Fail();
								}
								EndIf();
							} else {
								Statement(_Func("skipRange", name, _int(r.length)));
							}
							return true;
						}
					}
					if (isOneMore) {
						visit(inner, null);
					}
					While(MatchByteArray(byteset, false));
					{
						if (BinaryGrammar && byteset[0]) {
							If(_Func("eof"));
							{
								Break();
							}
							EndIf();
						}
						Statement(_Func("move", "1"));
					}
					EndWhile();
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
				if (inner instanceof Expression.PAny) {
					// Expression.Any e = (Expression.Any) inner;
					if (isOneMore) {
						visit(inner, null);
					}
					While(_Not(_Func("eof")));
					{
						Statement(_Func("move", "1"));
					}
					EndWhile();
					return true;
				}
			}
			return false;
		}

		private boolean tryAndOptimization(Expression inner) {
			if (Optimization) {
				if (inner instanceof Expression.PByte) {
					Expression.PByte e = (Expression.PByte) inner;
					If(_Func("prefetch"), _NotEq(), _byte(e.byteChar));
					{
						Fail();
					}
					EndIf();
					this.checkBinaryEOF(e.byteChar == 0);
					return true;
				}
				if (inner instanceof Expression.PByteSet) {
					Expression.PByteSet e = (Expression.PByteSet) inner;
					boolean[] byteset = e.byteSet();
					If(_Not(MatchByteArray(byteset, false)));
					{
						Fail();
					}
					EndIf();
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
				if (inner instanceof Expression.PAny) {
					// Expression.Any e = (Expression.Any) inner;
					If(_Func("eof"));
					{
						Fail();
					}
					EndIf();
					return true;
				}
			}
			return false;
		}

		private boolean tryNotOptimization(Expression inner) {
			if (Optimization) {
				if (inner instanceof Expression.PByte) {
					Expression.PByte e = (Expression.PByte) inner;
					If(_Func("prefetch"), _Eq(), _byte(e.byteChar));
					{
						Fail();
					}
					EndIf();
					this.checkBinaryEOF(e.byteChar != 0);
					return true;
				}
				if (inner instanceof Expression.PByteSet) {
					Expression.PByteSet e = (Expression.PByteSet) inner;
					boolean[] byteset = e.byteSet();
					If(MatchByteArray(byteset, false));
					{
						Fail();
					}
					EndIf();
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
				if (inner instanceof Expression.PAny) {
					// Expression.Any e = (Expression.Any) inner;
					If(_Not(_Func("eof")));
					{
						Fail();
					}
					EndIf();
					return true;
				}
			}
			return false;
		}

		/* Tree Construction */

		@Override
		public Object visitTree(Expression.PTree e, Object a) {
			if (e.folding) {
				Statement(_Func("foldTree", _int(e.beginShift), _label(e.label)));
			} else {
				Statement(_Func("beginTree", _int(e.beginShift)));
			}
			visit(e.get(0), a);
			Statement(_Func("endTree", _int(e.endShift), _tag(e.tag), _text(e.value)));
			return null;
		}

		@Override
		public Object visitLinkTree(Expression.PLinkTree e, Object a) {
			BeginScope();
			String tree = SaveTree();
			visit(e.get(0), a);
			Statement(_Func("linkTree", /* _Null(), */_label(e.label)));
			BackTree(tree);
			EndScope();
			return null;
		}

		@Override
		public Object visitTag(Expression.PTag e, Object a) {
			Statement(_Func("tagTree", _tag(e.tag)));
			return null;
		}

		@Override
		public Object visitReplace(Expression.PReplace e, Object a) {
			Statement(_Func("valueTree", _text(e.value)));
			return null;
		}

		@Override
		public Object visitDetree(Expression.PDetree e, Object a) {
			BeginScope();
			String n1 = SaveTree();
			String n2 = SaveLog();
			visit(e.get(0), a);
			BackTree(n1);
			BackLog(n2);
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolScope(Expression.PSymbolScope e, Object a) {
			BeginScope();
			String n = SaveSymbolTable();
			if (e.funcName == NezFunc.local) {
				Statement(_Func("addSymbolMask", _table(e.param)));
			}
			visit(e.get(0), a);
			BackSymbolTable(n);
			EndScope();
			return null;
		}

		@Override
		public Object visitSymbolAction(Expression.PSymbolAction e, Object a) {
			// BeginScope();
			// String ppos = SavePos();
			// visit(e.get(0), a);
			// Statement(_Func("addSymbol", _table(e.param), ppos));
			// EndScope();
			return null;
		}

		@Override
		public Object visitSymbolPredicate(Expression.PSymbolPredicate e, Object a) {
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
		public Object visitScan(Expression.PScan e, Object a) {
			BeginScope();
			String ppos = SavePos();
			visit(e.get(0), a);
			Statement(_Func("scanCount", ppos, _long(e.mask), _int(e.shift)));
			EndScope();
			return null;
		}

		@Override
		public Object visitRepeat(Expression.PRepeat e, Object a) {
			While(_Func("decCount"));
			{
				visit(e.get(0), a);
			}
			EndWhile();
			return null;
		}

		@Override
		public Object visitIf(Expression.PIfCondition e, Object a) {
			return null;
		}

		@Override
		public Object visitOn(Expression.POnCondition e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitTrap(Expression.PTrap e, Object a) {
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
		sb.append(_state());
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
		if (SupportedMatch2 && isMatchText(t, 2)) {
			return _Func("match2", _byte(t[0]), _byte(t[1]));
		}
		if (SupportedMatch3 && isMatchText(t, 3)) {
			return _Func("match3", _byte(t[0]), _byte(t[1]), _byte(t[2]));
		}
		if (SupportedMatch4 && isMatchText(t, 4)) {
			return _Func("match4", _byte(t[0]), _byte(t[1]), _byte(t[2]), _byte(t[3]));
		}
		if (SupportedMatch4 && isMatchText(t, 5)) {
			return _Func("match5", _byte(t[0]), _byte(t[1]), _byte(t[2]), _byte(t[3]), _byte(t[4]));
		}
		if (SupportedMatch4 && isMatchText(t, 6)) {
			return _Func("match6", _byte(t[0]), _byte(t[1]), _byte(t[2]), _byte(t[3]), _byte(t[4]), _byte(t[5]));
		}
		if (SupportedMatch4 && isMatchText(t, 7)) {
			return _Func("match7", _byte(t[0]), _byte(t[1]), _byte(t[2]), _byte(t[3]), _byte(t[4]), _byte(t[5]),
					_byte(t[6]));
		}
		if (SupportedMatch4 && isMatchText(t, 8)) {
			return _Func("match8", _byte(t[0]), _byte(t[1]), _byte(t[2]), _byte(t[3]), _byte(t[4]), _byte(t[5]),
					_byte(t[6]), _byte(t[7]));
		}
		return _Func("match", _text(t));
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
		return _argument(_state(), type(_state()));
	}

	protected String _funccall(String name) {
		return name + "(" + _state() + ")";
	}

	/* Statement */

	protected void BeginDecl(String line) {
		L(line);
		Begin();
	}

	protected void EndDecl() {
		End();
	}

	protected void BeginFunc(String type, String name, String args) {
		L();
		_L(_defun(type, name));
		_L("(");
		_L(args);
		_L(")");
		Begin();
	}

	protected final void BeginFunc(String f, String args) {
		BeginFunc(type("$parse"), f, args);
	}

	protected final void BeginFunc(String f) {
		BeginFunc(type("$parse"), f, _argument());
	}

	protected void EndFunc() {
		End();
	}

	protected void BeginLocalScope() {
		L("{");
		incIndent();
	}

	protected void EndLocalScope() {
		decIndent();
		L("}");
	}

	protected void Statement(String stmt) {
		L(stmt);
		_Semicolon();
	}

	protected void EmptyStatement() {
		L();
		_Semicolon();
	}

	protected void _Semicolon() {
		_L(";");
	}

	protected void Return(String expr) {
		Statement("return " + expr);
	}

	protected void Succ() {
		Return(_True());
	}

	protected void Fail() {
		Return(_False());
	}

	protected void If(String cond) {
		L("if (");
		_L(cond);
		_L(")");
		Begin();
	}

	protected String _Binary(String a, String op, String b) {
		return a + " " + op + " " + b;
	}

	protected void If(String a, String op, String b) {
		If(a + " " + op + " " + b);
	}

	protected void Else() {
		End();
		_L(" else");
		Begin();
	}

	protected void EndIf() {
		End();
	}

	protected void While(String cond) {
		L();
		_L("while (");
		_L(cond);
		_L(")");
		Begin();
	}

	protected void EndWhile() {
		End();
	}

	protected void Do() {
		L();
		_L("do");
		Begin();
	}

	protected void DoWhile(String cond) {
		End();
		_L("while (");
		_L(cond);
		_L(")");
		_Semicolon();
	}

	protected void Break() {
		L("break");
		_Semicolon();
	}

	protected void Switch(String c) {
		L("switch(" + c + ")");
		Begin();
	}

	protected void EndSwitch() {
		End();
	}

	protected void Case(String n) {
		L("case " + n + ": ");
	}

	protected void EndCase() {
	}

	protected void VarDecl(String name, String expr) {
		VarDecl(this.type(name), name, expr);
	}

	protected void VarDecl(String type, String name, String expr) {
		if (name == null) {
			VarAssign(name, expr);
		} else {
			Statement(type + " " + name + " = " + expr);
		}
	}

	protected void VarAssign(String v, String expr) {
		Statement(v + " = " + expr);
	}

	protected void DeclConst(String type, String name, String val) {
		if (type == null) {
			Statement("private final static " + name + " = " + val);
		} else {
			Statement("private final static " + type + " " + name + " = " + val);
		}
	}

	protected String _arity(int arity) {
		return "[" + arity + "]";
	}

	protected void DeclConst(String type, String name, int arity, String val) {
		if (type("$arity") != null) {
			DeclConst(type, name + _arity(arity), val);
		} else {
			DeclConst(type, name, val);
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
		return _Field(_state(), "pos");
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
		if (code.getMemoPointSize() > 0) {
			Statement(_Func("initMemo",
					"64/*FIXME*/"/*
									 * _int(strategy.SlidingWindow )
									 */, _int(code.getMemoPointSize())));
		}
	}
}
