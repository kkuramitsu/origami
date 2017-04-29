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
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PPair;

public class DispatchPass extends CommonPass {

	/* Choice Prediction */

	@Override
	public Expression visitChoice(PChoice choice, Void a) {
		ArrayList<Expression> bufferList = new ArrayList<>(256);
		HashMap<String, Byte> bufferIndex = new HashMap<>();
		ArrayList<Expression[]> uniqueList = new ArrayList<>();
		byte[] charMap = new byte[256];

		for (int ch = 0; ch < 256; ch++) {
			bufferList.clear();
			assert (bufferList.size() == 0);
			this.selectPredictedChoice(choice, ch, bufferList);
			if (bufferList.size() == 0) {
				charMap[ch] = 0;
				continue;
			}
			String key = this.key(bufferList);
			Byte index = bufferIndex.get(key);
			if (index == null) {
				uniqueList.add(bufferList.toArray(new Expression[bufferList.size()]));
				charMap[ch] = (byte) uniqueList.size();
				bufferIndex.put(key, charMap[ch]);
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

	private void selectPredictedChoice(PChoice choice, int ch, ArrayList<Expression> bufferList) {
		for (Expression e : choice) {
			Expression deref = Expression.deref(e);
			if (deref instanceof PChoice) {
				this.selectPredictedChoice((PChoice) deref, ch, bufferList);
			} else {
				ByteAcceptance acc = ByteAcceptance.acc(e, ch);
				if (acc != ByteAcceptance.Reject) {
					bufferList.add(e);
				}
			}
		}
	}

	private String key(ArrayList<Expression> bufferList) {
		StringBuilder sb = new StringBuilder();
		for (Expression e : bufferList) {
			sb.append(";");
			sb.append(e.toString());
		}
		return sb.toString();
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
		if (e instanceof PByte || e instanceof PByteSet || e instanceof PAny) {
			return true;
		}
		return false;
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
