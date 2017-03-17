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

package origami.nez.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import origami.nez.parser.pass.TreeCheckerPass;
import origami.nez.peg.Expression;
import origami.nez.peg.Expression.PNonTerminal;
import origami.nez.peg.Expression.PTrap;
import origami.nez.peg.ExpressionVisitor;
import origami.nez.peg.GrammarFlag;
import origami.nez.peg.NezFunc;
import origami.nez.peg.NonEmpty;
import origami.nez.peg.OGrammar;
import origami.nez.peg.OProduction;
import origami.trait.OStringUtils;

public class GrammarChecker {

	private final ParserFactory factory;
	private final OProduction start;
	private HashMap<String, OProduction> visited = new HashMap<>();
	private ArrayList<OProduction> prodList = new ArrayList<>();
	private TreeMap<String, Integer> flagMap = new TreeMap<>();

	public GrammarChecker(ParserFactory factory, OProduction start) {
		this.factory = factory;
		this.start = start;
	}

	static class ParserGrammar extends OGrammar {
		ParserGrammar(String name) {
			super(name);
		}

		@Override
		public String getUniqueName(String name) {
			return name;
		}
	}

	public OGrammar checkGrammar() {
		visitProduction(start);
		LeftRecursionChecker lrc = new LeftRecursionChecker(factory);
		TreeCheckerPass treeChecker = new TreeCheckerPass();
		for (OProduction p : prodList) {
			lrc.check(p.getExpression(), p);
			if (factory.is("strict-check", false)) {
				treeChecker.check(factory, p);
			}
		}
		OGrammar g = new ParserGrammar("@" + start.getUniqueName());
		String[] flags = this.flagNames();
		GrammarFlag.checkFlag(prodList, flags);
		EliminateFlags dup = new EliminateFlags(factory, g, flags);
		dup.duplicateName(start);
		// if (flags.length > 0) {
		// g.dump();
		// }
		factory.applyPass(g);
		return g;
	}

	private void visitProduction(OProduction p) {
		String key = p.getUniqueName();
		if (!visited.containsKey(key)) {
			visited.put(key, p);
			prodList.add(p);
			visitProduction(p.getExpression());
		}
	}

	private void visitProduction(Expression e) {
		if (e instanceof Expression.PNonTerminal) {
			PNonTerminal n = ((Expression.PNonTerminal) e);
			OProduction p = n.getProduction();
			if (p == null) {
				if (n.getLocalName().startsWith("\"")) {
					factory.reportError(e.getSourceLocation(), "undefined terminal: %s", n.getLocalName());
					n.getGrammar().addProduction(n.getLocalName(), Expression.newString(OStringUtils.unquoteString(n.getLocalName()), null));
				} else {
					factory.reportError(e.getSourceLocation(), "undefined %s", n.getLocalName());
					n.getGrammar().addProduction(n.getLocalName(), Expression.defaultFailure);
				}
				p = n.getProduction();
			}
			visitProduction(p);
			return;
		}
		if (this.usedChars != null) {
			if (e instanceof Expression.PByte) {
				usedChars[((Expression.PByte) e).byteChar] = true;
				return;
			}
			if (e instanceof Expression.PByteSet) {
				for (int i = 0; i < usedChars.length; i++) {
					if (((Expression.PByteSet) e).is(i)) {
						usedChars[i] = true;
					}
				}
				return;
			}
		}
		if (e instanceof Expression.PIfCondition) {
			this.countFlag(((Expression.PIfCondition) e).flagName());
		}
		for (Expression ei : e) {
			visitProduction(ei);
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
		for (String name : flagMap.keySet()) {
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

class LeftRecursionChecker extends ExpressionVisitor<Boolean, OProduction> {

	private final ParserFactory factory;

	LeftRecursionChecker(ParserFactory factory) {
		this.factory = factory;
	}

	boolean check(Expression e, OProduction a) {
		return e.visit(this, a);
	}

	@Override
	public Boolean visitNonTerminal(Expression.PNonTerminal e, OProduction a) {
		if (e.getUniqueName().equals(a.getUniqueName())) {
			factory.reportError(e.getSourceLocation(), "left recursion: " + a.getLocalName());
			e.isLeftRecursion = true;
			return true;
		}
		OProduction p = e.getProduction();
		return check(p.getExpression(), a);
	}

	@Override
	public Boolean visitEmpty(Expression.PEmpty e, OProduction a) {
		return true;
	}

	@Override
	public Boolean visitFail(Expression.PFail e, OProduction a) {
		return true;
	}

	@Override
	public Boolean visitByte(Expression.PByte e, OProduction a) {
		return false;
	}

	@Override
	public Boolean visitByteSet(Expression.PByteSet e, OProduction a) {
		return false;
	}

	@Override
	public Boolean visitAny(Expression.PAny e, OProduction a) {
		return false;
	}

	@Override
	public Boolean visitPair(Expression.PPair e, OProduction a) {
		if (check(e.get(0), a) == false) {
			return false;
		}
		return check(e.get(1), a);
	}

	@Override
	public Boolean visitChoice(Expression.PChoice e, OProduction a) {
		boolean unconsumed = false;
		for (Expression sub : e) {
			boolean c = check(sub, a);
			if (c == true) {
				unconsumed = true;
			}
		}
		return unconsumed;
	}

	@Override
	public Boolean visitDispatch(Expression.PDispatch e, OProduction a) {
		boolean unconsumed = false;
		for (int i = 1; i < e.size(); i++) {
			boolean c = check(e.get(i), a);
			if (c == true) {
				unconsumed = true;
			}
		}
		return unconsumed;
	}

	@Override
	public Boolean visitOption(Expression.POption e, OProduction a) {
		return true;
	}

	@Override
	public Boolean visitRepetition(Expression.PRepetition e, OProduction a) {
		if (e.isOneMore()) {
			return check(e.get(0), a);
		}
		return true;
	}

	@Override
	public Boolean visitAnd(Expression.PAnd e, OProduction a) {
		return true;
	}

	@Override
	public Boolean visitNot(Expression.PNot e, OProduction a) {
		return true;
	}

	@Override
	public Boolean visitTree(Expression.PTree e, OProduction a) {
		return check(e.get(0), a);
	}

	@Override
	public Boolean visitLinkTree(Expression.PLinkTree e, OProduction a) {
		return check(e.get(0), a);
	}

	@Override
	public Boolean visitTag(Expression.PTag e, OProduction a) {
		return true;
	}

	@Override
	public Boolean visitReplace(Expression.PReplace e, OProduction a) {
		return true;
	}

	@Override
	public Boolean visitDetree(Expression.PDetree e, OProduction a) {
		return check(e.get(0), a);
	}

	@Override
	public Boolean visitSymbolScope(Expression.PSymbolScope e, OProduction a) {
		return check(e.get(0), a);
	}

	@Override
	public Boolean visitSymbolAction(Expression.PSymbolAction e, OProduction a) {
		return check(e.get(0), a);
	}

	@Override
	public Boolean visitSymbolPredicate(Expression.PSymbolPredicate e, OProduction a) {
		if (e.funcName == NezFunc.exists) {
			return true;
		}
		return !NonEmpty.isAlwaysConsumed(e);
	}

	@Override
	public Boolean visitScan(Expression.PScan e, OProduction a) {
		return check(e.get(0), a);
	}

	@Override
	public Boolean visitRepeat(Expression.PRepeat e, OProduction a) {
		return true;
	}

	@Override
	public Boolean visitIf(Expression.PIfCondition e, OProduction a) {
		return true;
	}

	@Override
	public Boolean visitOn(Expression.POnCondition e, OProduction a) {
		return check(e.get(0), a);
	}

	@Override
	public Boolean visitTrap(PTrap e, OProduction a) {
		return true;
	}
}

@SuppressWarnings("serial")
class FlagContext extends TreeMap<String, Boolean> {

	final boolean flagLess;

	FlagContext(String[] flags, boolean defaultTrue) {
		super();
		flagLess = flags.length == 0;
		for (String c : flags) {
			this.put(c, defaultTrue);
		}
	}

	String contextualName(OProduction p, boolean nonTreeConstruction) {
		if (flagLess) {
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
	final ParserFactory fac;
	final FlagContext flagContext;

	EliminateFlags(ParserFactory factory, OGrammar grammar, String[] flags) {
		super(grammar);
		this.fac = factory;
		this.flagContext = new FlagContext(flags, false);
	}

	/* Conditional */

	private boolean isFlag(String flag) {
		return this.flagContext.get(flag);
	}

	private void setFlag(String flag, boolean b) {
		this.flagContext.put(flag, b);
	}

	public String duplicateName(OProduction sp) {
		String cname = flagContext.contextualName(sp, false);
		OProduction p = base.getProduction(cname);
		if (p == null) {
			base.addProduction(cname, Expression.defaultEmpty);
			base.setExpression(cname, dup(sp.getExpression(), null));
		}
		return cname;
	}

	@Override
	public Expression visitNonTerminal(PNonTerminal n, Void a) {
		String cname = duplicateName(n.getProduction());
		return new Expression.PNonTerminal(base, cname, ref(n));
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
		Boolean stackedFlag = isFlag(p.flagName());
		setFlag(p.flagName(), p.isPositive());
		Expression newe = dup(p.get(0), a);
		setFlag(p.flagName(), stackedFlag);
		return newe;
	}

	@Override
	public Expression visitIf(Expression.PIfCondition p, Void a) {
		if (isFlag(p.flagName())) { /* true */
			return p.isPositive() ? Expression.defaultEmpty : Expression.defaultFailure;
		}
		return p.isPositive() ? Expression.defaultFailure : Expression.defaultEmpty;
	}

}
