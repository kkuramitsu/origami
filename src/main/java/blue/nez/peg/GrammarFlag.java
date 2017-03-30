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

package blue.nez.peg;

import java.util.ArrayList;
import java.util.HashMap;

import blue.nez.peg.Expression.PIfCondition;
import blue.nez.peg.Expression.PNonTerminal;

public enum GrammarFlag {
	Yes, No, Unsure;

	public final static void checkFlag(ArrayList<Production> prodList, String[] flags) {
		for (String flag : flags) {
			checkFlag(prodList, flag);
		}
	}

	static void checkFlag(ArrayList<Production> prodList, String flag) {
		int size = prodList.size();
		int prev = 0;
		while (true) {
			int found = 0;
			for (Production p : prodList) {
				GrammarFlag f = getFlag(p.getGrammar(), p.getLocalName(), flag);
				if (f != Yes && f != No) {
					f = checkFlag(p.getGrammar(), p.getLocalName(), p.getExpression(), flag);
				}
				if (f == Yes || f == No) {
					found++;
				}
			}
			// ODebug.trace2("flag=%s %d %d %d", flag, prev, found, size);
			if (found == size || prev == found) {
				break;
			}
			prev = found;
		}
		for (Production p : prodList) {
			GrammarFlag f = getFlag(p.getGrammar(), p.getLocalName(), flag);
			if (f != Yes && f != No) {
				setFlag(p.getGrammar(), p.getLocalName(), flag, No);
			}
		}
	}

	static GrammarFlag checkFlag(Grammar g, String name, Expression e, String flag) {
		if (e instanceof PIfCondition) {
			PIfCondition p = (PIfCondition) e;
			if (p.flagName().equals(flag)) {
				setFlag(g, name, flag, Yes);
				return Yes;
			}
		}
		if (e instanceof PNonTerminal) {
			PNonTerminal p = (PNonTerminal) e;
			GrammarFlag f = getFlag(p.getGrammar(), p.getLocalName(), flag);
			if (f == Yes || f == No) {
				setFlag(g, name, flag, f);
				return f;
			}
			return Unsure;
		}
		boolean hasUnsure = false;
		for (Expression ei : e) {
			GrammarFlag f = checkFlag(g, name, ei, flag);
			if (f == Yes) {
				return Yes;
			}
			if (f != No) {
				hasUnsure = true;
			}
		}
		return hasUnsure ? Unsure : No;
	}

	@SuppressWarnings("serial")
	private static class FlagMap extends HashMap<String, GrammarFlag> {

	}

	static GrammarFlag getFlag(Grammar g, String name, String flag) {
		FlagMap map = g.getProperty(name, FlagMap.class);
		if (map == null) {
			map = new FlagMap();
			g.setProperty(name, map);
		}
		return map.get(flag);
	}

	static void setFlag(Grammar g, String name, String flag, GrammarFlag y) {
		FlagMap map = g.getProperty(name, FlagMap.class);
		if (map == null) {
			map = new FlagMap();
			g.setProperty(name, map);
		}
		map.put(flag, y);
	}

	public final static boolean hasFlag(Production p, String flag) {
		GrammarFlag f = getFlag(p.getGrammar(), p.getLocalName(), flag);
		assert f != null;
		return f != No;
	}

	// public final static GrammarFlag hasFlag(OGrammar g, String name,
	// Expression e, String flag) {
	// FlagMap map = g.getProperty(name, FlagMap.class);
	// if (map == null) {
	// map = new FlagMap();
	// g.setProperty(name, map);
	// }
	// GrammarFlag f = map.get(flag);
	// if (f == null) {
	// map.put(name, Unsure);
	// f = TrueFlagFunc.accept(e, flag);
	// map.put(flag, f);
	// }
	// return f;
	// }
	//
	// private static Analyzer TrueFlagFunc = new Analyzer();
	//
	// private final static class Analyzer extends
	// ExpressionVisitor<GrammarFlag, String> {
	//
	// public GrammarFlag accept(Expression e, String flag) {
	// return e.visit(this, flag);
	// }
	//
	// @Override
	// public GrammarFlag visitNonTerminal(Expression.PNonTerminal e, String
	// flag) {
	// Production p = e.getProduction();
	// return GrammarFlag.hasFlag(p.getGrammar(), p.getLocalName(),
	// p.getExpression(), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitEmpty(Expression.PEmpty e, String flag) {
	// return No;
	// }
	//
	// @Override
	// public GrammarFlag visitFail(Expression.PFail e, String flag) {
	// return No;
	// }
	//
	// @Override
	// public GrammarFlag visitByte(Expression.PByte e, String flag) {
	// return No;
	// }
	//
	// @Override
	// public GrammarFlag visitByteSet(Expression.PByteSet e, String flag) {
	// return No;
	// }
	//
	// @Override
	// public GrammarFlag visitAny(Expression.PAny e, String flag) {
	// return No;
	// // return Accept;
	// }
	//
	// @Override
	// public GrammarFlag visitPair(Expression.PPair e, String flag) {
	// GrammarFlag r = accept(e.get(0), flag);
	// if (r == No) {
	// return accept(e.get(1), flag);
	// }
	// return r;
	// }
	//
	// @Override
	// public GrammarFlag visitChoice(Expression.PChoice e, String flag) {
	// for (int i = 0; i < e.size(); i++) {
	// GrammarFlag r = accept(e.get(i), flag);
	// if (r != No) {
	// return r;
	// }
	// }
	// return No;
	// }
	//
	// @Override
	// public GrammarFlag visitDispatch(Expression.PDispatch e, String flag) {
	// for (int i = 0; i < e.size(); i++) {
	// GrammarFlag r = accept(e.get(i), flag);
	// if (r != No) {
	// return r;
	// }
	// }
	// return No;
	// }
	//
	// @Override
	// public GrammarFlag visitOption(Expression.POption e, String flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitRepetition(Expression.PRepetition e, String flag)
	// {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitAnd(Expression.PAnd e, String flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitNot(Expression.PNot e, String flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitTree(Expression.PTree e, String flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitLinkTree(Expression.PLinkTree e, String flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitTag(Expression.PTag e, String flag) {
	// return No;
	// }
	//
	// @Override
	// public GrammarFlag visitReplace(Expression.PReplace e, String flag) {
	// return No;
	// }
	//
	// @Override
	// public GrammarFlag visitDetree(Expression.PDetree e, String flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitSymbolScope(Expression.PSymbolScope e, String
	// flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitSymbolAction(Expression.PSymbolAction e, String
	// flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitSymbolPredicate(Expression.PSymbolPredicate e,
	// String flag) {
	// if (e.funcName == NezFunc.exists) {
	// return No;
	// }
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitScan(Expression.PScan e, String flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitRepeat(Expression.PRepeat e, String flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitIf(Expression.PIfCondition e, String flag) {
	// if (flag.equals(e.flagName())) {
	// return Yes;
	// }
	// return No;
	// }
	//
	// @Override
	// public GrammarFlag visitOn(Expression.POnCondition e, String flag) {
	// return accept(e.get(0), flag);
	// }
	//
	// @Override
	// public GrammarFlag visitTrap(Expression.PTrap e, String flag) {
	// return No;
	// }
	// }

}
