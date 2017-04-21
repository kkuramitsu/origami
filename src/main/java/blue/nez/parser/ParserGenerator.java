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

package blue.nez.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import blue.nez.ast.SourcePosition;
import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext.SymbolAction;
import blue.nez.parser.ParserContext.SymbolPredicate;
import blue.nez.parser.ParserContext.SymbolReset;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.Grammar;
import blue.nez.peg.Production;
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

public class ParserGenerator extends CodeBase implements OptionalFactory<ParserGenerator> {

	@Override
	public Class<?> keyClass() {
		return ParserGenerator.class;
	}

	@Override
	public ParserGenerator clone() {
		return this.newClone();
	}

	private OOption options;

	@Override
	public void init(OOption options) {
		this.options = options;
		this.defineSymbol("bool", "int");
		this.defineSymbol("true", "1");
		this.defineSymbol("false", "0");
	}

	protected final String getFileBaseName() {
		String file = this.options.stringValue(ParserOption.GrammarFile, "parser.opeg");
		return SourcePosition.extractFileBaseName(file);
	}

	public void generate(Grammar g) throws IOException {
		ParserGeneratorVisitor pgv = new ParserGeneratorVisitor();
		if (g instanceof ParserGrammar) {
			pgv.start((ParserGrammar) g, this);
		} else {
			Parser p = this.options == null ? g.newParser() : g.newParser(this.options);
			pgv.start(p.getParserGrammar(), this);
		}
		this.writeHeader();
		this.writeBody();
		this.writeFooter();
	}

	private void writeHeader() throws IOException {
		this.out.open(this.getFileBaseName() + ".h");
		this.out.importResourceContent("/blue/origami/include/cparser-runtime.c");
		this.out.println(this.head.toString());
	}

	private void writeBody() throws IOException {
		ArrayList<String> funcList = this.sortFuncList("start");
		for (String funcName : this.crossRefNames) {
			// System.out.println("prototype " + funcName);
			this.out.println("%s %s(Nez *px);", this.s("bool"), funcName);
		}
		for (String funcName : funcList) {
			SourceFragment s = this.codeMap.get(funcName);
			if (s != null) {
				if (!this.crossRefNames.contains(funcName)) {
					this.out.print("static inline");
				}
				this.out.p(s);
			}
		}
	}

	private void writeFooter() throws IOException {
		this.out.open(this.getFileBaseName() + ".c");
		this.out.println("#include \"%s.h\"", this.getFileBaseName());
		this.out.importResourceContent("/blue/origami/include/cparser-main.c");
	}

	@Override
	protected void defineConst(String typeName, String constName, String literal) {
		int loc = typeName.indexOf("[");
		if (loc > 0) { // T[10] a => T a[10]
			constName = constName + typeName.substring(loc);
			typeName = typeName.substring(0, loc);
		}
		this.L("%s %s = %s;", typeName, constName, literal);
	}

	public void beginDefine(String funcName, Expression e) {
		this.openFragment(funcName);
		//
		this.currentFunc = funcName;
		this.L("%s %s(Nez *px) {", this.s("bool"), funcName);
		this.L("// " + e);
		this.incIndent();
		this.L("%s %s = %s;", this.s("bool"), this.s("r"), this.s("true"));
	}

	public void endDefine() {
		this.L("return %s;", this.s("r"));
		this.decIndent();
		this.L("}");
		//
		this.closeFlagment();
	}

	public void matchSucc() {
		this.L("%s = %s;", this.s("r"), this.s("true"));
	}

	public void matchFail() {
		this.L("%s = %s;", this.s("r"), this.s("false"));
	}

	public void matchAny() {
		this.L("%s = !Nez_eof(%s);", this.s("r"), this.s("px"));
	}

	public void matchByte(int uchar) {
		this.L("%s = (Nez_read(%s) == %s);", this.s("r"), this.s("px"), uchar);
	}

	public void matchByteSet(ByteSet byteSet) {
		String constName = this.getConstName("static int[8]", this.toLiteral(byteSet));
		this.L("%s = Nez_bitis(%s, %s, Nez_read(%s));", this.s("r"), this.s("px"), constName, this.s("px"));
	}

	private String currentFunc = null;

	public void matchNonTerminal(String func) {
		this.addFunctionDependency(this.currentFunc, func);
		this.L("%s = %s(%s);", this.s("r"), func, this.s("px"));
	}

	private int u = 0;

	public int unique() {
		return this.u++;
	}

	public void pushLoadPos(int uid) {
		this.L("const unsigned char* %s = Nez_pos(px);", this.s("pos") + uid);
	}

	public void loadTree(int uid) {
		this.L("Tree *%s = Nez_loadTree(px);", this.s("left") + uid);
	}

	public void loadTreeLog(int uid) {
		this.L("size_t %s = Nez_loadTreeLog(px);", this.s("tlog") + uid);
	}

	public void loadSymbolTable(int uid) {
		this.L("void *%s = Nez_loadSymbolTable(px);", this.s("state") + uid);
	}

	public void updatePos(int uid) {
		this.L("%s = Nez_pos(px);", this.s("pos") + uid);
	}

	public void updateTree(int uid) {
		this.L("%s = Nez_loadTree(px);", this.s("left") + uid);
	}

	public void updateTreeLog(int uid) {
		this.L("%s = Nez_loadTreeLog(px);", this.s("tlog") + uid);
	}

	public void updateSymbolTable(int uid) {
		this.L("%s = Nez_loadSymbolTable(px);", this.s("state") + uid);
	}

	public void storePos(int uid) {
		this.L("Nez_setpos(px, %s);", this.s("pos") + uid);

	}

	public void storeTree(int uid) {
		this.L("Nez_storeTree(px, %s);", this.s("left") + uid);
	}

	public void storeTreeLog(int uid) {
		this.L("Nez_storeTreeLog(px, %s);", this.s("tlog") + uid);
	}

	public void storeSymbolTable(int uid) {
		this.L("Nez_storeSymbolTable(px, %s);", this.s("state") + uid);
	}

	public void beginIfTrue() {
		this.L("if(%s) {", this.s("r"));
		this.incIndent();
	}

	public void beginIfFailure() {
		this.L("if(!%s) {", this.s("r"));
		this.incIndent();
	}

	public void orElse() {
		this.decIndent();
		this.L("} else {");
		this.incIndent();
	}

	public void endIf() {
		this.decIndent();
		this.L("}");
	}

	public void beginWhileTrue() {
		this.L("while(%s) {", this.s("r"));
		this.incIndent();

	}

	public void endWhile() {
		this.decIndent();
		this.L("}");
	}

	public void tagTree(Symbol tag) {
		this.L("Nez_tagTree(%s, %s);", this.s("px"), this.toLiteral(tag));
	}

	public void valueTree(String value) {
		byte[] b = OStringUtils.utf8(value);
		this.L("Nez_valueTree(%s, %s, %d);", this.s("px"), this.toLiteral(value), b.length);
	}

	public void linkTree(int uid, Symbol label) {
		this.L("Nez_linkTree(%s, %s, %s);", this.s("px"), this.s("left") + uid, this.toLiteral(label));

	}

	public void foldTree(int beginShift, Symbol label) {
		this.L("Nez_foldTree(%s, %d, %s);", this.s("px"), beginShift, this.toLiteral(label));
	}

	public void beginTree(int beginShift) {
		this.L("Nez_beginTree(%s, %d);", this.s("px"), beginShift);
	}

	public void endTree(int endShift, Symbol tag, String value) {
		byte[] b = OStringUtils.utf8(value);
		this.L("Nez_endTree(%s, %d, %s, %s, %d);", this.s("px"), endShift, this.toLiteral(tag), this.toLiteral(value),
				b.length);
	}

	public void callSymbolAction(SymbolAction action, Symbol label) {
		// TODO Auto-generated method stub

	}

	public void callSymbolAction(SymbolAction action, Symbol label, int uid) {
		// TODO Auto-generated method stub

	}

	public void callSymbolPredicate(SymbolPredicate pred, Symbol label, Object option) {
		// TODO Auto-generated method stub

	}

	public void callSymbolPredicate(SymbolPredicate pred, Symbol label, int uid, Object option) {
		// TODO Auto-generated method stub

	}

	public void pushIndexMap(int uid, byte[] indexMap) {
		String constName = this.getConstName("static int[256]", this.toLiteral(indexMap));
		this.L("int %s = %s[Nez_prefetch(%s)];", this.s("d") + uid, constName, this.s("px"));
	}

	public void beginSwitch(int uid) {
		this.L("switch(%s) {", this.s("d") + uid);
		this.incIndent();
	}

	public void endSwitch() {
		this.decIndent();
		this.L("}");

	}

	public void beginCase(int uid, int i) {
		this.L("case %d: {", i);
		this.incIndent();

	}

	public void endCase() {
		this.L("break;");
		this.decIndent();
		this.L("}");
	}

	private String toLiteral(String s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatStringLiteral(sb, '"', s, '"');
			return sb.toString();
		}
		return this.s("NULL");
	}

	private String toLiteral(Symbol s) {
		if (s != null) {
			StringBuilder sb = new StringBuilder();
			OStringUtils.formatStringLiteral(sb, '"', s.toString(), '"');
			return sb.toString();
		}
		return this.s("NULL");
	}

	private String toLiteral(byte[] indexMap) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (byte index : indexMap) {
			sb.append(index & 0xff);
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

	private String toLiteral(ByteSet bs) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (int index : bs.bits()) {
			sb.append(index);
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

	public void initCounter(int uid) {
		this.L("int %s = 0;", "c" + uid);
	}

	public void countCounter(int uid) {
		this.L("%s++;", "c" + uid);

	}

	public void checkCounter(int uid) {
		this.L("if(%s == 0) %s = %s;", "c" + uid, this.s("r"), this.s("false"));
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

	@Override
	public String toString() {
		return this.sb.toString();
	}
}

abstract class CodeBase {

	protected SourceFragment head = new SourceFragment();

	protected void initHeader() {
		this.head = new SourceFragment();
	}

	private SourceFragment body;

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
								System.out.println("Cyclic " + key + " => " + nextNode);
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

class ParserGeneratorVisitor extends ExpressionVisitor<Boolean, ParserGenerator> {

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
				px.beginDefine(funcName, e);
				this.matchInline(e, px);
				px.endDefine();
			}
		}
	}

	public void matchInline(Expression e, ParserGenerator px) {
		e.visit(this, px);
	}

	public void match(Expression e, ParserGenerator px) {
		if (this.isInline(e)) {
			e.visit(this, px);
		} else {
			String funcName = px.getFuncName(e);
			this.waitingList.add(e);
			px.matchNonTerminal(funcName);
		}
	}

	private boolean isInline(Expression e) {
		if (e instanceof PByte || e instanceof PByteSet || e instanceof PAny || e instanceof PNonTerminal) {
			return true;
		}
		if (e instanceof PTag || e instanceof PReplace || e instanceof PEmpty || e instanceof PFail) {
			return true;
		}
		return false;
	}

	@Override
	public Boolean visitNonTerminal(PNonTerminal e, ParserGenerator px) {
		String funcName = px.getFuncName(e);
		this.waitingList.add(e);
		px.matchNonTerminal(funcName);
		return null;
	}

	@Override
	public Boolean visitEmpty(PEmpty e, ParserGenerator px) {
		px.matchSucc();
		return null;
	}

	@Override
	public Boolean visitFail(PFail e, ParserGenerator px) {
		px.matchFail();
		return null;
	}

	@Override
	public Boolean visitByte(PByte e, ParserGenerator px) {
		px.matchByte(e.byteChar());
		return null;
	}

	@Override
	public Boolean visitByteSet(PByteSet e, ParserGenerator px) {
		px.matchByteSet(e.byteSet());
		return null;
	}

	@Override
	public Boolean visitAny(PAny e, ParserGenerator px) {
		px.matchAny();
		return null;
	}

	@Override
	public Boolean visitPair(PPair e, ParserGenerator px) {
		this.match(e.get(0), px);
		px.beginIfTrue();
		this.match(e.get(1), px);
		px.endIf();
		return null;
	}

	private boolean isStateful(Expression e) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isCons(Expression e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Boolean visitChoice(PChoice e, ParserGenerator px) {
		int uid = px.unique();
		px.pushLoadPos(uid);
		if (this.isCons(e)) {
			px.loadTree(uid);
			px.loadTreeLog(uid);
		}
		if (this.isStateful(e)) {
			px.loadSymbolTable(uid);
		}
		this.match(e.get(0), px);
		for (int i = 1; i < e.size(); i++) {
			px.beginIfFailure();
			px.storePos(uid);
			if (this.isCons(e)) {
				px.storeTree(uid);
				px.storeTreeLog(uid);
			}
			if (this.isStateful(e)) {
				px.storeSymbolTable(uid);
			}
			this.match(e.get(i), px);
		}
		for (int i = 1; i < e.size(); i++) {
			px.endIf();
		}
		return null;
	}

	@Override
	public Boolean visitDispatch(PDispatch e, ParserGenerator px) {
		int uid = px.unique();
		px.pushIndexMap(uid, e.indexMap);
		px.beginSwitch(uid);
		for (int i = 0; i < e.size(); i++) {
			px.beginCase(uid, i);
			this.match(e.get(i), px);
			px.endCase();
		}
		px.endSwitch();
		return null;
	}

	@Override
	public Boolean visitOption(POption e, ParserGenerator px) {
		int uid = px.unique();
		px.pushLoadPos(uid);
		if (this.isCons(e.get(0))) {
			px.loadTree(uid);
			px.loadTreeLog(uid);
		}
		if (this.isStateful(e.get(0))) {
			px.loadSymbolTable(uid);
		}
		this.match(e.get(0), px);
		px.beginIfFailure();
		px.storePos(uid);
		if (this.isCons(e.get(0))) {
			px.storeTree(uid);
			px.storeTreeLog(uid);
		}
		if (this.isStateful(e.get(0))) {
			px.storeSymbolTable(uid);
		}
		px.matchSucc();
		px.endIf();
		return null;
	}

	@Override
	public Boolean visitRepetition(PRepetition e, ParserGenerator px) {
		int uid = px.unique();
		px.pushLoadPos(uid);
		if (this.isCons(e.get(0))) {
			px.loadTree(uid);
			px.loadTreeLog(uid);
		}
		if (this.isStateful(e.get(0))) {
			px.loadSymbolTable(uid);
		}
		if (e.isOneMore()) {
			px.initCounter(uid);
		}
		px.beginWhileTrue();
		this.match(e.get(0), px);
		// px.checkEmpty(uid, e.get(0));
		px.beginIfTrue();
		px.updatePos(uid);
		if (this.isCons(e.get(0))) {
			px.updateTree(uid);
			px.updateTreeLog(uid);
		}
		if (this.isStateful(e.get(0))) {
			px.updateSymbolTable(uid);
		}
		if (e.isOneMore()) {
			px.countCounter(uid);
		}
		px.orElse();
		px.storePos(uid);
		if (this.isCons(e.get(0))) {
			px.storeTree(uid);
			px.storeTreeLog(uid);
		}
		if (this.isStateful(e.get(0))) {
			px.storeSymbolTable(uid);
		}
		px.endIf();
		px.endWhile();
		if (e.isOneMore()) {
			px.checkCounter(uid);
		}
		return null;
	}

	@Override
	public Boolean visitAnd(PAnd e, ParserGenerator px) {
		int uid = px.unique();
		px.pushLoadPos(uid);
		if (this.isCons(e.get(0))) {
			px.loadTree(uid);
		}
		this.match(e.get(0), px);
		px.beginIfTrue();
		px.storePos(uid);
		if (this.isCons(e.get(0))) {
			px.storeTree(uid);
		}
		px.endIf();
		return null;
	}

	@Override
	public Boolean visitNot(PNot e, ParserGenerator px) {
		int uid = px.unique();
		px.pushLoadPos(uid);
		if (this.isCons(e.get(0))) {
			px.loadTree(uid);
			px.loadTreeLog(uid);
		}
		if (this.isStateful(e.get(0))) {
			px.loadSymbolTable(uid);
		}
		this.match(e.get(0), px);
		px.beginIfTrue();
		px.matchFail();
		px.orElse();
		px.storePos(uid);
		if (this.isCons(e.get(0))) {
			px.storeTree(uid);
			px.storeTreeLog(uid);
		}
		if (this.isStateful(e.get(0))) {
			px.storeSymbolTable(uid);
		}
		px.matchSucc();
		px.endIf();
		return null;
	}

	@Override
	public Boolean visitTree(PTree e, ParserGenerator px) {
		if (e.folding) {
			px.foldTree(e.beginShift, e.label);
		} else {
			px.beginTree(e.beginShift);
		}
		this.matchInline(e.get(0), px);
		px.beginIfTrue();
		px.endTree(e.endShift, e.tag, e.value);
		px.endIf();
		return null;
	}

	@Override
	public Boolean visitDetree(PDetree e, ParserGenerator px) {
		int uid = px.unique();
		px.loadTree(uid);
		px.loadTreeLog(uid);
		this.match(e.get(0), px);
		px.beginIfTrue();
		px.storeTree(uid);
		px.storeTreeLog(uid);
		px.endIf();
		return null;
	}

	@Override
	public Boolean visitLinkTree(PLinkTree e, ParserGenerator px) {
		int uid = px.unique();
		if (this.isCons(e.get(0))) {
			px.loadTree(uid);
			px.loadTreeLog(uid);
			this.match(e.get(0), px);
			px.beginIfTrue();
			px.storeTreeLog(uid);
			px.linkTree(uid, e.label);
			px.storeTree(uid);
			px.endIf();
		} else {
			this.match(e.get(0), px);
		}
		return null;
	}

	@Override
	public Boolean visitTag(PTag e, ParserGenerator px) {
		px.tagTree(e.tag);
		return null;
	}

	@Override
	public Boolean visitReplace(PReplace e, ParserGenerator px) {
		px.valueTree(e.value);
		return null;
	}

	@Override
	public Boolean visitSymbolScope(PSymbolScope e, ParserGenerator px) {
		int uid = px.unique();
		px.loadSymbolTable(uid);
		if (e.label != null) {
			px.callSymbolAction(new SymbolReset(), e.label);
		}
		this.match(e.get(0), px);
		px.beginIfTrue();
		px.storeSymbolTable(uid);
		px.endIf();
		return null;
	}

	@Override
	public Boolean visitSymbolAction(PSymbolAction e, ParserGenerator px) {
		if (e.isEmpty()) {
			px.callSymbolAction(e.action, e.label);
		} else {
			int uid = px.unique();
			px.pushLoadPos(uid);
			this.match(e.get(0), px);
			px.beginIfTrue();
			px.callSymbolAction(e.action, e.label, uid);
			px.endIf();
		}
		return null;
	}

	@Override
	public Boolean visitSymbolPredicate(PSymbolPredicate e, ParserGenerator px) {
		if (e.isEmpty()) {
			px.callSymbolPredicate(e.pred, e.label, e.option);
		} else {
			int uid = px.unique();
			px.pushLoadPos(uid);
			this.match(e.get(0), px);
			px.beginIfTrue();
			px.callSymbolPredicate(e.pred, e.label, uid, e.option);
			px.endIf();
		}
		return null;
	}

	@Override
	public Boolean visitIf(PIfCondition e, ParserGenerator px) {
		// Symbol label = Symbol.unique(e.flagName());
		// boolean b = new SymbolExist().match(px, label, px.pos, null);
		// return e.isPositive() ? b : !b;
		return null;
	}

	@Override
	public Boolean visitOn(POnCondition e, ParserGenerator px) {
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
		return null;
	}

	@Override
	public Boolean visitScan(PScan e, ParserGenerator px) {
		// int ppos = px.pos;
		// if (!this.parse(e.get(0), px)) {
		// return false;
		// }
		// px.scanCount(ppos, e.mask, e.shift);
		return null;
	}

	@Override
	public Boolean visitRepeat(PRepeat e, ParserGenerator px) {
		// // int ppos = px.pos;
		// while (this.parse(e.get(0), px)) {
		// if (!px.decCount()) {
		// return true;
		// }
		// }
		return null;
	}

	@Override
	public Boolean visitTrap(PTrap e, ParserGenerator px) {
		return null;
	}

}
