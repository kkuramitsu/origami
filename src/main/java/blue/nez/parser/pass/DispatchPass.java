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
		ArrayList<Expression> selected = new ArrayList<>(256);
		ArrayList<String> keyList = new ArrayList<>(256);
		HashMap<String, Byte> indexMap = new HashMap<>();
		ArrayList<Expression[]> uniqueList = new ArrayList<>();
		byte[] charMap = new byte[256];
		for (int ch = 0; ch < 256; ch++) {
			selected.clear();
			keyList.clear();
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
		// int oc = 1;
		// selected.clear();
		// indexMap.clear();
		// for (Expression[] seq : uniqueList) {
		// Expression joined = this.mergeExpression(seq);
		// String key = joined.toString();
		// if (!indexMap.containsKey(key)) {
		// indexMap.put(key, (byte) selected.size());
		// selected.add(joined);
		// this.remap(charMap, oc, selected.size());
		// } else {
		// this.remap(charMap, oc, indexMap.get(key));
		// }
		// oc++;
		// }
		// int c = 0;
		// Expression[] inners = new Expression[selected.size()];
		// for (Expression e : selected) {
		// inners[c] = e;
		// c++;
		// }
		int c = 0;
		Expression[] inners = new Expression[uniqueList.size()];
		for (Expression[] seq : uniqueList) {
			inners[c] = this.mergeExpression(seq);
			c++;
		}
		return this.optimized(choice, new PDispatch(inners, charMap));
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

	private void selectPredictedChoice(PChoice choice, int ch, ArrayList<String> keyList,
			ArrayList<Expression> selected) {
		for (Expression e : choice) {
			Expression deref = Expression.deref(e);
			if (deref instanceof PChoice) {
				this.selectPredictedChoice((PChoice) deref, ch, keyList, selected);
			} else {
				ByteAcceptance acc = ByteAcceptance.acc(e, ch);
				if (acc != ByteAcceptance.Reject) {
					this.append(keyList, selected, deref);
				}
			}
		}
	}

	private void append(ArrayList<String> keyList, ArrayList<Expression> selected, Expression e) {
		if (e instanceof PFail) {
			return;
		}
		Expression first = this.firstExpression2(e);
		if (PDispatch.isConsumed(e)) {
			e = Expression.newSequence(first, this.nextExpression2(e));
		}
		String key = this.key(e);
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
		if (PDispatch.isConsumed(e)) {
			sb.append(". ''");
			// this.nextExpression(e).strOut(sb);
			return;
		}
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
