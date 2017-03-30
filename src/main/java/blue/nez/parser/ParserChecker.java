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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import blue.nez.parser.pass.DispatchPass;
import blue.nez.parser.pass.InlinePass;
import blue.nez.parser.pass.NotCharPass;
import blue.nez.parser.pass.TreeCheckerPass;
import blue.nez.parser.pass.TreePass;
import blue.nez.peg.Expression;
import blue.nez.peg.Expression.PNonTerminal;
import blue.nez.peg.Expression.PTrap;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.Grammar;
import blue.nez.peg.GrammarFlag;
import blue.nez.peg.NezFunc;
import blue.nez.peg.NonEmpty;
import blue.nez.peg.Production;
import blue.origami.util.ODebug;
import blue.origami.util.OOption;
import blue.origami.util.OStringUtils;

public class ParserChecker {

	private final Production start;
	private final OOption options;
	private HashMap<String, Production> visited = new HashMap<>();
	private ArrayList<Production> prodList = new ArrayList<>();
	private TreeMap<String, Integer> flagMap = new TreeMap<>();

	public ParserChecker(OOption options, Production start) {
		this.options = options;
		this.start = start;
	}

	static class ParserGrammar extends Grammar {
		ParserGrammar(String name) {
			super(name);
		}

		@Override
		public String getUniqueName(String name) {
			return name;
		}
	}

	public Grammar checkGrammar() {
		this.visitProduction(this.start);
		LeftRecursionChecker lrc = new LeftRecursionChecker(this.options);
		TreeCheckerPass treeChecker = new TreeCheckerPass();
		for (Production p : this.prodList) {
			lrc.check(p.getExpression(), p);
			if (this.options.is(ParserOption.StrictChecker, false)) {
				treeChecker.check(p, this.options);
			}
		}
		Grammar g = new ParserGrammar("@" + this.start.getUniqueName());
		String[] flags = this.flagNames();
		GrammarFlag.checkFlag(this.prodList, flags);
		EliminateFlags dup = new EliminateFlags(this.options, g, flags);
		dup.duplicateName(this.start);
		// if (flags.length > 0) {
		// g.dump();
		// }
		return this.applyPass(g);
	}

	private Grammar applyPass(Grammar g) {
		if (this.options.is(ParserOption.Unoptimized, false)) {
			return g;
		}
		String[] pass = this.options.list(ParserOption.Pass);
		if (pass.length > 0) {
			return this.applyPass(g, this.loadPassClass(pass));
		} else {
			return this.applyPass(g, NotCharPass.class, TreePass.class, DispatchPass.class, InlinePass.class);
		}
	}

	private Class<?>[] loadPassClass(String[] pass) {
		ArrayList<Class<?>> l = new ArrayList<>();
		for (String p : pass) {
			try {
				l.add(this.options.loadClass(p, this.options.list(ParserOption.PassPath)));
			} catch (ClassNotFoundException e) {
				ODebug.traceException(e);
			}
		}
		return l.toArray(new Class<?>[l.size()]);
	}

	private Grammar applyPass(Grammar g, Class<?>... classes) {
		for (Class<?> c : classes) {
			try {
				ParserPass pass = (ParserPass) c.newInstance();
				long t1 = this.options.nanoTime(null, 0);
				g = pass.perform(g, this.options);
				this.options.nanoTime("Pass: " + pass, t1);
			} catch (InstantiationException | IllegalAccessException e) {
				ODebug.traceException(e);
			}
		}
		return g;
	}

	private void visitProduction(Production p) {
		String key = p.getUniqueName();
		if (!this.visited.containsKey(key)) {
			this.visited.put(key, p);
			this.prodList.add(p);
			this.visitProduction(p.getExpression());
		}
	}

	private void visitProduction(Expression e) {
		if (e instanceof Expression.PNonTerminal) {
			PNonTerminal n = ((Expression.PNonTerminal) e);
			Production p = n.getProduction();
			if (p == null) {
				if (n.getLocalName().startsWith("\"")) {
					this.options.reportError(e.getSourceLocation(), "undefined terminal: %s", n.getLocalName());
					n.getGrammar().addProduction(n.getLocalName(),
							Expression.newString(OStringUtils.unquoteString(n.getLocalName()), null));
				} else {
					this.options.reportError(e.getSourceLocation(), "undefined %s", n.getLocalName());
					n.getGrammar().addProduction(n.getLocalName(), Expression.defaultFailure);
				}
				p = n.getProduction();
			}
			this.visitProduction(p);
			return;
		}
		if (this.usedChars != null) {
			if (e instanceof Expression.PByte) {
				this.usedChars[((Expression.PByte) e).byteChar] = true;
				return;
			}
			if (e instanceof Expression.PByteSet) {
				for (int i = 0; i < this.usedChars.length; i++) {
					if (((Expression.PByteSet) e).is(i)) {
						this.usedChars[i] = true;
					}
				}
				return;
			}
		}
		if (e instanceof Expression.PIfCondition) {
			this.countFlag(((Expression.PIfCondition) e).flagName());
		}
		for (Expression ei : e) {
			this.visitProduction(ei);
		}
	}

	// public final List<Production> getProductions() {
	// return this.list;
	// }

	// private HashMap<String, Integer> countMap = null;
	private boolean[] usedChars = null;

	// public void enabledNonterminalCount() {
	// this.countMap = new HashMap<>();
	// }
	//
	// private void countNonterminal(String key) {
	// Integer n = this.countMap.get(key);
	// if (n == null) {
	// n = 1;
	// } else {
	// n = n + 1;
	// }
	// this.countMap.put(key, n);
	// }
	//
	// public int getNonterminalCount(String uname) {
	// Integer n = this.countMap.get(uname);
	// return n == null ? 0 : n;
	// }
	//
	// public void enabledFlagNames() {
	// this.flagMap = new TreeMap<>();
	// }

	private void countFlag(String key) {
		Integer n = this.flagMap.get(key);
		if (n == null) {
			n = 1;
		} else {
			n = n + 1;
		}
		this.flagMap.put(key, n);
	}

	public final String[] flagNames() {
		String[] n = new String[this.flagMap.size()];
		int c = 0;
		for (String name : this.flagMap.keySet()) {
			n[c] = name;
			c++;
		}
		return n;
	}

	public void enabledCharList() {
		this.usedChars = new boolean[256];
	}

	public final boolean isBinary() {
		return this.usedChars[0] = true;
	}

	// private HashMap<String, HashSet<Production>> reachMap = null;
	//
	// public void enabledMatrix() {
	// this.reachMap = new HashMap<>();
	// }
	//
	// private void computeReach() {
	// for (Production p : list) {
	// HashSet<Production> memo = new HashSet<>();
	// visitReach(p.getExpression(), memo);
	// if (memo.size() > 0) {
	// reachMap.put(p.getUniqueName(), memo);
	// }
	// }
	// for (String key : reachMap.keySet()) {
	// HashSet<Production> visited = this.reachMap.get(key);
	// for (Production pp : visited) {
	// visitReach2(pp.getExpression(), visited);
	// }
	// }
	// }
	//
	// private void visitReach(Expression p, HashSet<Production> memo) {
	// if (p instanceof Expression.PNonTerminal) {
	// memo.add(((Expression.PNonTerminal) p).getProduction());
	// return;
	// }
	// for (Expression e : p) {
	// visitReach(e, memo);
	// }
	// }
	//
	// private void visitReach2(Expression p, HashSet<Production> visited) {
	// if (p instanceof Expression.PNonTerminal) {
	// Production pp = ((Expression.PNonTerminal) p).getProduction();
	// if (!visited.contains(pp)) {
	// visited.add(pp);
	// visitReach2(pp.getExpression(), visited);
	// }
	// return;
	// }
	// for (Expression e : p) {
	// visitReach2(e, visited);
	// }
	// }
}

class Stat {

}

class LeftRecursionChecker extends ExpressionVisitor<Boolean, Production> {

	private final OOption options;

	LeftRecursionChecker(OOption options) {
		this.options = options;
	}

	boolean check(Expression e, Production a) {
		return e.visit(this, a);
	}

	@Override
	public Boolean visitNonTerminal(Expression.PNonTerminal e, Production a) {
		if (e.getUniqueName().equals(a.getUniqueName())) {
			this.options.reportError(e.getSourceLocation(), "left recursion: " + a.getLocalName());
			// e.isLeftRecursion = true;
			return true;
		}
		Production p = e.getProduction();
		return this.check(p.getExpression(), a);
	}

	@Override
	public Boolean visitEmpty(Expression.PEmpty e, Production a) {
		return true;
	}

	@Override
	public Boolean visitFail(Expression.PFail e, Production a) {
		return true;
	}

	@Override
	public Boolean visitByte(Expression.PByte e, Production a) {
		return false;
	}

	@Override
	public Boolean visitByteSet(Expression.PByteSet e, Production a) {
		return false;
	}

	@Override
	public Boolean visitAny(Expression.PAny e, Production a) {
		return false;
	}

	@Override
	public Boolean visitPair(Expression.PPair e, Production a) {
		if (this.check(e.get(0), a) == false) {
			return false;
		}
		return this.check(e.get(1), a);
	}

	@Override
	public Boolean visitChoice(Expression.PChoice e, Production a) {
		boolean unconsumed = false;
		for (Expression sub : e) {
			boolean c = this.check(sub, a);
			if (c == true) {
				unconsumed = true;
			}
		}
		return unconsumed;
	}

	@Override
	public Boolean visitDispatch(Expression.PDispatch e, Production a) {
		boolean unconsumed = false;
		for (int i = 1; i < e.size(); i++) {
			boolean c = this.check(e.get(i), a);
			if (c == true) {
				unconsumed = true;
			}
		}
		return unconsumed;
	}

	@Override
	public Boolean visitOption(Expression.POption e, Production a) {
		return true;
	}

	@Override
	public Boolean visitRepetition(Expression.PRepetition e, Production a) {
		if (e.isOneMore()) {
			return this.check(e.get(0), a);
		}
		return true;
	}

	@Override
	public Boolean visitAnd(Expression.PAnd e, Production a) {
		return true;
	}

	@Override
	public Boolean visitNot(Expression.PNot e, Production a) {
		return true;
	}

	@Override
	public Boolean visitTree(Expression.PTree e, Production a) {
		return this.check(e.get(0), a);
	}

	@Override
	public Boolean visitLinkTree(Expression.PLinkTree e, Production a) {
		return this.check(e.get(0), a);
	}

	@Override
	public Boolean visitTag(Expression.PTag e, Production a) {
		return true;
	}

	@Override
	public Boolean visitReplace(Expression.PReplace e, Production a) {
		return true;
	}

	@Override
	public Boolean visitDetree(Expression.PDetree e, Production a) {
		return this.check(e.get(0), a);
	}

	@Override
	public Boolean visitSymbolScope(Expression.PSymbolScope e, Production a) {
		return this.check(e.get(0), a);
	}

	@Override
	public Boolean visitSymbolAction(Expression.PSymbolAction e, Production a) {
		return this.check(e.get(0), a);
	}

	@Override
	public Boolean visitSymbolPredicate(Expression.PSymbolPredicate e, Production a) {
		if (e.funcName == NezFunc.exists) {
			return true;
		}
		return !NonEmpty.isAlwaysConsumed(e);
	}

	@Override
	public Boolean visitScan(Expression.PScan e, Production a) {
		return this.check(e.get(0), a);
	}

	@Override
	public Boolean visitRepeat(Expression.PRepeat e, Production a) {
		return true;
	}

	@Override
	public Boolean visitIf(Expression.PIfCondition e, Production a) {
		return true;
	}

	@Override
	public Boolean visitOn(Expression.POnCondition e, Production a) {
		return this.check(e.get(0), a);
	}

	@Override
	public Boolean visitTrap(PTrap e, Production a) {
		return true;
	}
}

@SuppressWarnings("serial")
class FlagContext extends TreeMap<String, Boolean> {

	final boolean flagLess;

	FlagContext(String[] flags, boolean defaultTrue) {
		super();
		this.flagLess = flags.length == 0;
		for (String c : flags) {
			this.put(c, defaultTrue);
		}
	}

	String contextualName(Production p, boolean nonTreeConstruction) {
		if (this.flagLess) {
			return p.getUniqueName();
		} else {
			StringBuilder sb = new StringBuilder();
			// if (nonTreeConstruction) {
			// sb.append("~");
			// }
			sb.append(p.getUniqueName());
			for (String flag : this.keySet()) {
				if (GrammarFlag.hasFlag(p, flag)) {
					if (this.get(flag)) {
						sb.append("&");
						sb.append(flag);
					}
				}
			}
			String cname = sb.toString();
			// System.out.println("flags: " + this.keySet());
			// System.out.println(p.getUniqueName() + "=>" + cname);
			return cname;
		}
	}
}

class EliminateFlags extends Expression.Duplicator<Void> {
	final OOption fac;
	final FlagContext flagContext;

	EliminateFlags(OOption options, Grammar grammar, String[] flags) {
		super(grammar);
		this.fac = options;
		this.flagContext = new FlagContext(flags, false);
	}

	/* Conditional */

	private boolean isFlag(String flag) {
		return this.flagContext.get(flag);
	}

	private void setFlag(String flag, boolean b) {
		this.flagContext.put(flag, b);
	}

	public String duplicateName(Production sp) {
		String cname = this.flagContext.contextualName(sp, false);
		Production p = this.base.getProduction(cname);
		if (p == null) {
			this.base.addProduction(cname, Expression.defaultEmpty);
			this.base.setExpression(cname, this.dup(sp.getExpression(), null));
		}
		return cname;
	}

	@Override
	public Expression visitNonTerminal(PNonTerminal n, Void a) {
		String cname = this.duplicateName(n.getProduction());
		return new Expression.PNonTerminal(this.base, cname, this.ref(n));
	}

	@Override
	public Expression visitOn(Expression.POnCondition p, Void a) {
		// if (fac.is("strict-check", false)) {
		// if (!GrammarFlag.hasFlag(p.get(0), p.flagName())) {
		// fac.reportWarning(p.getSourceLocation(), "unused condition: " +
		// p.flagName());
		// return dup(p.get(0), a);
		// }
		// }
		Boolean stackedFlag = this.isFlag(p.flagName());
		this.setFlag(p.flagName(), p.isPositive());
		Expression newe = this.dup(p.get(0), a);
		this.setFlag(p.flagName(), stackedFlag);
		return newe;
	}

	@Override
	public Expression visitIf(Expression.PIfCondition p, Void a) {
		if (this.isFlag(p.flagName())) { /* true */
			return p.isPositive() ? Expression.defaultEmpty : Expression.defaultFailure;
		}
		return p.isPositive() ? Expression.defaultFailure : Expression.defaultEmpty;
	}

}
