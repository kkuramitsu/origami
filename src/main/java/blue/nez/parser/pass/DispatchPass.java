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
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PPair;

public class DispatchPass extends CommonPass {

	@Override
	public Expression visitChoice(PChoice choice, Void a) {
		// Expression e1 = this.visitChoice1(choice, a);
		Expression e2 = this.visitChoice2(choice, a);
		// if (!e1.equals(e2)) {
		// System.out.println("## " + e1.size() + ", " + e2.size());
		// System.out.println(e1);
		// System.out.println(e2);
		// }
		return e2;
	}

	Expression visitChoice2(PChoice choice, Void a) {
		ArrayList<Expression> selected = new ArrayList<>(256);
		ArrayList<String> keyList = new ArrayList<>(256);
		this.expandChoice(choice, keyList, selected);
		ArrayList<Expression> selected2 = new ArrayList<>(selected.size());
		ArrayList<String> keyList2 = new ArrayList<>(keyList.size());
		HashMap<String, Byte> indexMap = new HashMap<>();
		ArrayList<Expression[]> uniqueList = new ArrayList<>();
		byte[] charMap = new byte[256];
		for (int ch = 0; ch < 256; ch++) {
			selected2.clear();
			keyList2.clear();
			for (int i = 0; i < selected.size(); i++) {
				Expression e = selected.get(i);
				ByteAcceptance acc = ByteAcceptance.acc(e, ch);
				if (acc != ByteAcceptance.Reject) {
					selected2.add(e);
					keyList2.add(keyList.get(i));
				}
			}
			if (selected2.size() == 0) {
				charMap[ch] = 0;
				continue;
			}
			String key = this.joinKeys(keyList2);
			Byte index = indexMap.get(key);
			if (index == null) {
				uniqueList.add(selected2.toArray(new Expression[selected2.size()]));
				charMap[ch] = (byte) uniqueList.size();
				indexMap.put(key, charMap[ch]);
			} else {
				charMap[ch] = index;
			}
		}
		int oc = 1;
		selected2.clear();
		indexMap.clear();
		for (Expression[] seq : uniqueList) {
			Expression joined = this.mergeExpression(seq);
			String key = joined.toString();
			if (!indexMap.containsKey(key)) {
				indexMap.put(key, (byte) selected2.size());
				selected2.add(joined);
				this.remap(charMap, oc, selected2.size());
			} else {
				this.remap(charMap, oc, indexMap.get(key));
			}
			oc++;
		}
		int c = 0;
		int max = 1;
		Expression[] inners = new Expression[selected2.size()];
		for (Expression e : selected2) {
			inners[c] = e;
			if (e instanceof PChoice && max < e.size()) {
				max = e.size();
			}
			c++;
		}
		// this.options.verbose("choice %d => %d", choice.size(), max);
		// if (max >= 2) {
		// this.options.verbose("choice %s", choice);
		// }
		return this.optimized(choice, new PDispatch(inners, charMap));
	}

	private void expandChoice(PChoice choice, ArrayList<String> keyList, ArrayList<Expression> selected) {
		for (Expression p : choice) {
			Expression e = Expression.deref(p);
			if (e instanceof PChoice) {
				this.expandChoice((PChoice) e, keyList, selected);
			} else {
				if (e instanceof PFail) {
					return;
				}
				Expression first = this.firstExpression2(e);
				if (PDispatch.isConsumed(e)) {
					e = Expression.newSequence(first, this.nextExpression2(e));
				}
				String key = this.key2(e);
				keyList.add(key);
				selected.add(e);
			}
		}
	}

	private String key2(Expression e) {
		StringBuilder sb = new StringBuilder();
		if (PDispatch.isConsumed(e)) {
			sb.append(". ''");
			return sb.toString();
		}
		if (e instanceof PPair) {
			if (PDispatch.isConsumed(e.get(0))) {
				sb.append(". ");
				e.get(1).strOut(sb);
				return sb.toString();
			}
		}
		e.strOut(sb);
		return sb.toString();
	}

	private String joinKeys(ArrayList<String> keyList) {
		if (keyList.size() == 1) {
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

	private void remap(byte[] charMap, int oc, int nc) {
		if (oc != nc) {
			for (int i = 0; i < charMap.length; i++) {
				if (charMap[i] == oc) {
					charMap[i] = (byte) nc;
				}
			}
		}
	}

	Expression visitChoice1(PChoice choice, Void a) {
		ArrayList<Expression> selected = new ArrayList<>(256);
		ArrayList<String> keyList = new ArrayList<>(256);
		HashMap<String, Byte> indexMap = new HashMap<>();
		ArrayList<Expression[]> uniqueList = new ArrayList<>();
		byte[] charMap = new byte[256];
		for (int ch = 0; ch < 256; ch++) {
			selected.clear();
			keyList.clear();
			this.selectPredictedChoice1(choice, ch, keyList, selected);
			if (selected.size() == 0) {
				charMap[ch] = 0;
				continue;
			}
			String key = this.joinKeys(keyList);
			Byte index = indexMap.get(key);
			if (index == null) {
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

	private void selectPredictedChoice1(PChoice choice, int ch, ArrayList<String> keyList,
			ArrayList<Expression> selected) {
		for (Expression e : choice) {
			Expression deref = Expression.deref(e);
			if (deref instanceof PChoice) {
				this.selectPredictedChoice1((PChoice) deref, ch, keyList, selected);
			} else {
				ByteAcceptance acc = ByteAcceptance.acc(e, ch);
				if (acc != ByteAcceptance.Reject) {
					this.append1(keyList, selected, deref);
				}
			}
		}
	}

	private void append1(ArrayList<String> keyList, ArrayList<Expression> selected, Expression e) {
		if (e instanceof PFail) {
			return;
		}
		String key = this.key1(e);
		keyList.add(key);
		selected.add(e);
	}

	private String key1(Expression e) {
		StringBuilder sb = new StringBuilder();
		e.strOut(sb);
		return sb.toString();
	}

	private Expression mergeExpression(Expression[] seq) {
		if (seq.length == 1) {
			if (PDispatch.isConsumed(seq[0])) {
				return Expression.defaultAny;
			}
			return seq[0];
		}
		List<Expression> l = Expression.newList(seq.length);
		if (this.isAllComsumedPrefix(seq)) {
			for (Expression e : seq) {
				Expression.addChoice(l, this.nextExpression2(e));
			}
			return this.subChoice(l);
		} else {
			List<Expression> sub = Expression.newList(seq.length);
			for (Expression e : seq) {
				if (this.isCharacterConsumed(e)) {
					Expression.addChoice(sub, this.nextExpression2(e));
				} else {
					if (sub.size() > 0) {
						Expression.addChoice(l, this.subChoice(sub));
						sub.clear();
					}
					Expression.addChoice(l, e);
				}
			}
			if (sub.size() > 0) {
				Expression.addChoice(l, this.subChoice(sub));
				sub.clear();
			}
			Expression e = Expression.newChoice(l);
			// System.out.println(e);
			return e;
		}
	}

	private Expression subChoice(List<Expression> l) {
		Expression choice = Expression.newChoice(l);
		// System.out.println(choice);
		if (choice instanceof PChoice) {
			choice = this.visitChoice((PChoice) choice, null);
		}
		return Expression.newSequence(Expression.defaultAny, choice);
	}

	private boolean isAllComsumedPrefix(Expression[] seq) {
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

	private Expression firstExpression2(Expression e) {
		e = Expression.deref(e);
		if (e instanceof PPair) {
			return this.firstExpression2(Expression.deref(e.get(0)));
		}
		return e;
	}

	private Expression nextExpression2(Expression e) {
		return this.nextExpression2(e, Expression.defaultEmpty);
	}

	private Expression nextExpression2(Expression e, Expression next) {
		e = Expression.deref(e);
		if (e instanceof PPair) {
			Expression first = Expression.deref(e.get(0));
			return this.nextExpression2(first, Expression.newSequence(e.get(1), next));
		}
		return next;
	}

}
