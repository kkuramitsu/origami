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

package origami.main;

import java.util.HashMap;

import origami.OConsole;
import origami.nez.parser.NZ86ParserContext;

import origami.nez.parser.TrapAction;
import origami.nez.peg.Expression;
import origami.nez.peg.Grammar;
import origami.nez.peg.Production;

public class Otest extends Oexample {

	@Override
	protected void initOption(OOption options) {
		super.initOption(options);
		options.set(ParserOption.Coverage, true);
	}

	public static class Coverage {
		HashMap<String, Integer> unameMap = null;
		String[] names;
		int[] enterCounts;
		int[] exitCounts;

		public Coverage() {
		}

		public void init(OOption options, Grammar g) {
			Production[] prods = g.getAllProductions();
			this.unameMap = new HashMap<>();
			this.names = new String[prods.length];
			this.enterCounts = new int[prods.length];
			this.exitCounts = new int[prods.length];
			options.add(ParserOption.TrapActions, new TrapAction[] { this.newEnterAction(), this.newExitAction() });
			int enterId = 0;
			int exitId = 1;
			int uid = 0;
			for (Production p : prods) {
				this.names[uid] = p.getUniqueName();
				Expression enterTrap = new Expression.PTrap(enterId, uid, null);
				Expression exitTrap = new Expression.PTrap(exitId, uid++, null);
				Expression e = Expression.append(p.getExpression(), exitTrap);
				g.setExpression(p.getLocalName(), Expression.newSequence(enterTrap, e, null));
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
			public void performed(NZ86ParserContext<?> context, int uid) {
				enterCounts[uid]++;

			}
		}

		class ExitAction implements TrapAction {
			@Override
			public void performed(NZ86ParserContext<?> context, int uid) {
				exitCounts[uid]++;
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
			for (int i = 0; i < names.length; i++) {
				if (enterCounts[i] > 0) {
					c++;
				}
			}
			return ((double) c) / names.length;
		}

		public final double cov() {
			int c = 0;
			for (int i = 0; i < names.length; i++) {
				if (exitCounts[i] > 0) {
					c++;
				}
			}
			return ((double) c) / names.length;
		}

		public void dump(OOption options) {
			for (int i = 0; i < names.length; i++) {
				if (exitCounts[i] == 0) {
					OConsole.println("%s %d/%d", names[i], enterCounts[i], exitCounts[i]);
				}
			}
			OConsole.println("Coverage %.3f %.3f", enterCov(), cov());
		}

	}

}
