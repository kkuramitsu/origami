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
import java.util.Map;

import blue.nez.peg.Grammar;
import blue.nez.peg.Production;
import blue.nez.peg.Typestate;
import blue.origami.util.OOption;
import blue.origami.util.OptionalFactory;

public abstract class ParserCode<I> implements ParserExecutable {

	protected final OOption options;
	protected final ParserGrammar grammar;
	protected ArrayList<I> codeList;
	protected HashMap<String, I> codeMap;

	protected ParserCode(ParserGrammar grammar, OOption options, I[] initArray) {
		this.options = options;
		this.grammar = grammar;
		this.codeList = initArray != null ? new ArrayList<>() : null;
		this.codeMap = new HashMap<>();
	}

	@Override
	public final ParserGrammar getGrammar() {
		return this.grammar;
	}

	public final I getStartInstruction() {
		return this.codeList.get(0);
	}

	public final void setInstruction(String uname, I inst) {
		this.codeMap.put(uname, inst);
	}

	public final I getInstruction(String uname) {
		return this.codeMap.get(uname);
	}

	public final int getInstructionSize() {
		return this.codeList.size();
	}

	/* MemoPoint */

	public final static class MemoPoint {
		public final int id;
		public final String label;
		public final Typestate typeState;
		final boolean contextSensitive;

		public MemoPoint(int id, String label, Typestate typeState, boolean contextSensitive) {
			this.id = id;
			this.label = label;
			this.typeState = typeState;
			this.contextSensitive = contextSensitive;
		}

		public final boolean isStateful() {
			return this.contextSensitive;
		}

		public final Typestate getTypestate() {
			return this.typeState;
		}

		int memoHit = 0;
		int memoFailHit = 0;
		long hitLength = 0;
		int maxLength = 0;
		int memoMiss = 0;

		public void memoHit(int consumed) {
			this.memoHit += 1;
			this.hitLength += consumed;
			if (this.maxLength < consumed) {
				this.maxLength = consumed;
			}
		}

		public void failHit() {
			this.memoFailHit += 1;
		}

		public void miss() {
			this.memoMiss++;
		}

		public final double hitRatio() {
			if (this.memoMiss == 0) {
				return 0.0;
			}
			return (double) this.memoHit / this.memoMiss;
		}

		public final double failHitRatio() {
			if (this.memoMiss == 0) {
				return 0.0;
			}
			return (double) this.memoFailHit / this.memoMiss;
		}

		public final double meanLength() {
			if (this.memoHit == 0) {
				return 0.0;
			}
			return (double) this.hitLength / this.memoHit;
		}

		public final int count() {
			return this.memoMiss + this.memoFailHit + this.memoHit;
		}

		protected final boolean checkDeactivation() {
			if (this.memoMiss == 32) {
				if (this.memoHit < 2) {
					return true;
				}
			}
			if (this.memoMiss % 64 == 0) {
				if (this.memoHit == 0) {
					return true;
				}
				if (this.memoMiss / this.memoHit > 10) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return this.label + "[id=" + this.id + "]";
		}

	}

	protected Map<String, MemoPoint> memoPointMap = null;

	public final MemoPoint getMemoPoint(String uname) {
		if (this.memoPointMap != null) {
			return this.memoPointMap.get(uname);
		}
		return null;
	}

	public final int getMemoPointSize() {
		return this.memoPointMap != null ? this.memoPointMap.size() : 0;
	}

	public void initMemoPoint() {
		StaticMemoization memo = this.options.newInstance(StaticMemoization.class);
		this.memoPointMap = new HashMap<>();
		memo.init(this.grammar, this.memoPointMap);
	}

	public static class StaticMemoization implements OptionalFactory<StaticMemoization> {
		public void init(Grammar grammar, Map<String, MemoPoint> memoPointMap) {
			for (Production p : grammar) {
				Typestate ts = Typestate.compute(p);
				if (ts == Typestate.Tree) {
					String uname = p.getUniqueName();
					MemoPoint memoPoint = new MemoPoint(memoPointMap.size(), uname, ts, false);
					memoPointMap.put(uname, memoPoint);
				}
			}
		}

		@Override
		public Class<?> keyClass() {
			return StaticMemoization.class;
		}

		@Override
		public StaticMemoization clone() {
			return new StaticMemoization();
		}

		protected OOption options;

		@Override
		public void init(OOption options) {
			this.options = options;
		}
	}

	public final void dumpMemoPoints() {
		if (this.memoPointMap != null) {
			this.options.verbose("ID\tPEG\tCount\tHit\tFail\tMean");
			for (String key : this.memoPointMap.keySet()) {
				MemoPoint p = this.memoPointMap.get(key);
				this.options.verbose("%d\t%s\t%d\t%f\t%f\t%f", p.id, p.label, p.count(), p.hitRatio(), p.failHitRatio(),
						p.meanLength());
			}
			this.options.verbose("");
		}
	}

	abstract public void dump();
}
