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

import java.util.List;

import blue.nez.ast.Symbol;
import blue.nez.peg.Expression;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PReplace;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTree;

public class TreePass extends CommonPass {

	@Override
	public Expression visitTree(PTree tree, Void a) {
		List<Expression> l = Expression.newList(64);
		Expression.addSequence(l, rewrite(tree.get(0), a));
		List<Expression> factored = Expression.newList(l.size());
		List<Expression> inners = Expression.newList(l.size());
		int beginShift = tree.beginShift;
		Symbol foundTag = null;
		String foundReplace = null;
		int i;
		for (i = 0; i < l.size(); i++) {
			Expression e = l.get(i);
			if (e instanceof PTag) {
				foundTag = ((PTag) e).tag;
				continue;
			}
			if (e instanceof PReplace) {
				foundReplace = ((PReplace) e).value;
				continue;
			}
			int len = consumed(e, 0);
			if (len == -1) {
				break;
			}
			beginShift -= len;
			factored.add(e);
		}
		boolean movableTag = true;
		boolean movableReplace = true;
		for (; i < l.size(); i++) {
			Expression e = l.get(i);
			if (movableTag) {
				if (e instanceof PTag) {
					foundTag = ((PTag) e).tag;
					continue;
				}
				movableTag = checkTag(e, 0);
				if (!movableTag) {
					foundTag = null;
				}
			}
			if (movableReplace) {
				if (e instanceof PReplace) {
					foundReplace = ((PReplace) e).value;
					continue;
				}
				movableReplace = checkReplace(e, 0);
				if (!movableReplace) {
					foundReplace = null;
				}
			}
			inners.add(e);
		}
		if (tree.tag != null) {
			foundTag = tree.tag;
		}
		if (tree.value != null) {
			foundReplace = tree.value;
		}
		Expression inner = Expression.newSequence(inners, ref(tree));
		PTree newTree = new PTree(tree.folding, tree.label, beginShift, inner, foundTag,
				foundReplace, tree.endShift, ref(tree));
		return optimized(tree, Expression.newSequence(Expression.newSequence(factored, ref(tree)), newTree, null));
	}

	private int consumed(Expression e, int depth) {
		if (e instanceof PNot || e instanceof PEmpty) {
			return 0;
		}
		if (e instanceof PByte || e instanceof PAny || e instanceof PByteSet) {
			return 1;
		}
		if (e instanceof PNonTerminal) {
			if (depth < 10) {
				return consumed(Expression.deref(e), depth + 1);
			}
			return -1;
		}
		if (e instanceof PPair) {
			int len = consumed(e.get(0), depth);
			if (len == -1) {
				return -1;
			}
			int len2 = consumed(e.get(1), depth);
			if (len2 == -1) {
				return -1;
			}
			return len + len2;
		}
		if (e instanceof PChoice) {
			int len = consumed(e.get(0), depth);
			if (len == -1) {
				return -1;
			}
			for (Expression sub : e) {
				if (len != consumed(sub, depth)) {
					return -1;
				}
			}
			return -1;
		}
		return -1;
	}

	private boolean checkTag(Expression e, int depth) {
		if (e instanceof PNonTerminal) {
			if (depth < 10) {
				return checkTag(Expression.deref(e), depth + 1);
			}
			return false; // unchecked
		}
		if (e instanceof PTag) {
			return false;
		}
		for (Expression sub : e) {
			if (checkTag(sub, depth) == false) {
				return false;
			}
		}
		return true;
	}

	private boolean checkReplace(Expression e, int depth) {
		if (e instanceof PNonTerminal) {
			if (depth < 10) {
				return checkTag(Expression.deref(e), depth + 1);
			}
			return false; // unchecked
		}
		if (e instanceof PReplace) {
			return false;
		}
		for (Expression sub : e) {
			if (checkReplace(sub, depth) == false) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Expression visitPair(PPair pair, Void a) {
		if (pair.get(0) instanceof PTree) {
			PTree tree = (PTree) pair.get(0);
			List<Expression> inners = Expression.newList(64);
			Expression.addSequence(inners, tree.get(0));
			List<Expression> l = Expression.newList(64);
			Expression.addSequence(l, rewrite(pair.get(1), a));
			List<Expression> remained = Expression.newList(l.size());
			int endShift = tree.endShift;
			int i;
			for (i = 0; i < l.size(); i++) {
				Expression e = l.get(i);
				int len = consumed(e, 0);
				if (len == -1) {
					break;
				}
				endShift -= len;
				inners.add(e);
			}
			for (; i < l.size(); i++) {
				remained.add(l.get(i));
			}
			tree = new PTree(tree.folding, tree.label, tree.beginShift, Expression.newSequence(inners, null),
					tree.tag, tree.value, endShift, null);
			return optimized(pair,
					Expression.newSequence(visitTree(tree, a), Expression.newSequence(remained, null), null));
		}
		return super.visitPair(pair, a);
	}

	@Override
	public Expression visitLinkTree(PLinkTree p, Void a) {
		if (p.get(0) instanceof PChoice) {
			Expression choice = p.get(0);
			List<Expression> l = Expression.newList(choice.size());
			for (Expression inner : choice) {
				inner = rewrite(inner, a);
				l.add(new PLinkTree(p.label, inner, null));
			}
			return optimized(p, Expression.newChoice(l, null));
		}
		return super.visitLinkTree(p, a);
	}

}
