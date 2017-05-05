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

package blue.nez.parser.pass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import blue.nez.peg.ByteAcceptance;
import blue.nez.peg.Expression;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PPair;

public class DispatchPass extends CommonPass {

	@Override
	public Expression visitChoice(PChoice choice, Void a) {
		ArrayList<Expression> selected = new ArrayList<>(256);
		ArrayList<String> keyList = new ArrayList<>(256);
		HashMap<String, Byte> indexMap = new HashMap<>();
		ArrayList<Expression[]> uniqueList = new ArrayList<>();
		byte[] charMap = new byte[256];

		for (int ch = 0; ch < 256; ch++) {
			selected.clear();
			this.selectPredictedChoice(choice, ch, keyList, selected);
			if (selected.size() == 0) {
				charMap[ch] = 0;
				continue;
			}
			String key = this.joinKeys(keyList);
			Byte index = indexMap.get(key);
			if (index == null) {
				// System.out.println("key=" + key);
				uniqueList.add(selected.toArray(new Expression[selected.size()]));
				charMap[ch] = (byte) uniqueList.size();
				indexMap.put(key, charMap[ch]);
			} else {
				charMap[ch] = index;
			}
		}
		int c = 0;
		Expression[] inners = new Expression[uniqueList.size()];
		for (Expression[] seq : uniqueList) {
			inners[c] = this.mergeExpression(seq);
			c++;
		}
		return this.optimized(choice, new PDispatch(inners, charMap));
	}

	private void selectPredictedChoice(PChoice choice, int ch, ArrayList<String> keyList,
			ArrayList<Expression> selected) {
		for (Expression e : choice) {
			Expression deref = Expression.deref(e);
			if (deref instanceof PChoice) {
				this.selectPredictedChoice((PChoice) deref, ch, keyList, selected);
			} else {
				ByteAcceptance acc = ByteAcceptance.acc(e, ch);
				if (acc != ByteAcceptance.Reject) {
					this.append(keyList, selected, e);
				}
			}
		}
	}

	private void append(ArrayList<String> keyList, ArrayList<Expression> selected, Expression e) {
		String key = this.key(e);
		// for (String key2 : keyList) {
		// if (key2.equals(key)) {
		// System.out.println("e2 == e:" + key2 + ", " + e);
		// return;
		// }
		// }
		keyList.add(key);
		selected.add(e);
	}

	private String joinKeys(ArrayList<String> keyList) {
		if (keyList.size() == 0) {
			return keyList.get(0);
		} else {
			StringBuilder sb = new StringBuilder();
			for (String key2 : keyList) {
				sb.append(";");
				sb.append(key2);
			}
			return sb.toString();
		}
	}

	private String key(Expression e) {
		StringBuilder sb = new StringBuilder();
		this.appendKey(e, sb);
		return sb.toString();
	}

	private void appendKey(Expression e, StringBuilder sb) {
		// I don't know why it is not working
		// if (PDispatch.isConsumed(e)) {
		// sb.append(".");
		// return;
		// }
		if (e instanceof PPair) {
			if (PDispatch.isConsumed(e.get(0))) {
				sb.append(". ");
				e.get(1).strOut(sb);
				return;
			}
		}
		e.strOut(sb);
	}

	private Expression mergeExpression(Expression[] seq) {
		if (seq.length == 1) {
			return seq[0];
		}
		List<Expression> l = Expression.newList(seq.length);
		if (this.isCommonPrefix(seq)) {
			for (Expression e : seq) {
				Expression.addChoice(l, this.nextExpression(e));
			}
			Expression choice = Expression.newChoice(l);
			if (choice instanceof PChoice) {
				choice = this.visitChoice((PChoice) choice, null);
			}
			return Expression.newSequence(Expression.defaultAny, choice);
		} else {
			for (Expression e : seq) {
				Expression.addChoice(l, e);
			}
			return Expression.newChoice(l);
		}
	}

	private boolean isCommonPrefix(Expression[] seq) {
		for (Expression e : seq) {
			if (!this.isCharacterConsumed(e)) {
				return false;
			}
		}
		return true;
	}

	private boolean isCharacterConsumed(Expression e) {
		e = Expression.deref(e);
		if (e instanceof PPair) {
			e = Expression.deref(e.get(0));
		}
		return PDispatch.isConsumed(e);
	}

	private Expression nextExpression(Expression e) {
		e = Expression.deref(e);
		if (e instanceof PPair) {
			Expression first = Expression.deref(e.get(0));
			if (first instanceof PPair) {
				List<Expression> l = Expression.newList(16);
				Expression.addSequence(l, first.get(1));
				Expression.addSequence(l, e.get(1));
				return Expression.newSequence(l);
			}
			return e.get(1);
		}
		return Expression.defaultEmpty;
	}

}
