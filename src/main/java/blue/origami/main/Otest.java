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

package blue.origami.main;

import java.util.ArrayList;
import java.util.HashMap;

import blue.origami.nez.parser.ParserContext;
import blue.origami.nez.parser.TrapAction;
import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.Grammar;
import blue.origami.nez.peg.Production;
import blue.origami.nez.peg.expression.PTrap;
import blue.origami.util.OConsole;
import blue.origami.util.OOption;

public class Otest extends Oexample {

	@Override
	protected void initOption(OOption options) {
		super.initOption(options);
		options.set(MainOption.Coverage, true);
		options.set(MainOption.ThrowingParserError, true);
		options.set(MainOption.PartialFailure, true);
	}

	public static class Coverage {
		HashMap<String, Integer> unameMap = null;
		String[] names;
		int[] enterCounts;
		int[] exitCounts;

		public Coverage() {
		}

		// public void init(OOption options, Grammar g) {
		// Production[] prods = g.getAllProductions();
		// this.unameMap = new HashMap<>();
		// this.names = new String[prods.length];
		// this.enterCounts = new int[prods.length];
		// this.exitCounts = new int[prods.length];
		// options.add(ParserOption.TrapActions, new TrapAction[] {
		// this.newEnterAction(), this.newExitAction() });
		// int enterId = 0;
		// int exitId = 1;
		// int uid = 0;
		// for (Production p : prods) {
		// this.names[uid] = p.getUniqueName();
		// Expression enterTrap = new Expression.PTrap(enterId, uid, null);
		// Expression exitTrap = new Expression.PTrap(exitId, uid++, null);
		// Expression e = Expression.append(p.getExpression(), exitTrap);
		// g.setExpression(p.getLocalName(), Expression.newSequence(enterTrap,
		// e, null));
		// }
		// }

		public void init(OOption options, Grammar g) {
			this.unameMap = new HashMap<>();
			ArrayList<String> nameList = new ArrayList<>();
			options.add(MainOption.TrapActions, new TrapAction[] { this.newEnterAction(), this.newExitAction() });
			int enterId = 0;
			int exitId = 1;
			this.init(g, enterId, exitId, nameList);
			this.names = nameList.toArray(new String[nameList.size()]);
			this.enterCounts = new int[nameList.size()];
			this.exitCounts = new int[nameList.size()];
		}

		private void init(Grammar g, int enterId, int exitId, ArrayList<String> nameList) {
			for (Production p : g) {
				int uid = nameList.size();
				nameList.add(p.getUniqueName());
				Expression enterTrap = new PTrap(enterId, uid);
				Expression exitTrap = new PTrap(exitId, uid++);
				Expression e = Expression.append(p.getExpression(), exitTrap);
				g.setExpression(p.getLocalName(), Expression.newSequence(enterTrap, e));
			}
			for (Grammar lg : g.getLocalGrammars()) {
				this.init(lg, enterId, exitId, nameList);
			}
		}

		public int uid(String uname) {
			Integer u = this.unameMap.get(uname);
			if (u == null) {
				u = this.unameMap.size();
				this.unameMap.put(uname, u);
			}
			return u;
		}

		class EnterAction implements TrapAction {
			@Override
			public void performed(ParserContext px, int uid) {
				Coverage.this.enterCounts[uid]++;

			}
		}

		class ExitAction implements TrapAction {
			@Override
			public void performed(ParserContext px, int uid) {
				Coverage.this.exitCounts[uid]++;
			}
		}

		public TrapAction newEnterAction() {
			return new EnterAction();
		}

		public TrapAction newExitAction() {
			return new ExitAction();
		}

		public final double enterCov() {
			int c = 0;
			for (int i = 0; i < this.names.length; i++) {
				if (this.enterCounts[i] > 0) {
					c++;
				}
			}
			return ((double) c) / this.names.length;
		}

		public final double cov() {
			int c = 0;
			for (int i = 0; i < this.names.length; i++) {
				if (this.exitCounts[i] > 0) {
					c++;
				}
			}
			return ((double) c) / this.names.length;
		}

		public void dump(OOption options) {
			for (int i = 0; i < this.names.length; i++) {
				if (this.exitCounts[i] == 0) {
					OConsole.println("%s %d/%d", this.names[i], this.enterCounts[i], this.exitCounts[i]);
				}
			}
			OConsole.println("Coverage %.3f %.3f", this.enterCov(), this.cov());
		}

	}

}
