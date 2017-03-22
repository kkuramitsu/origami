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

package origami.nez.parser.pass;

import java.util.List;

import origami.nez.ast.Symbol;
import origami.nez.peg.Expression;

public class TreePass extends CommonPass {

	@Override
	public Expression visitTree(Expression.PTree tree, Void a) {
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
			if (e instanceof Expression.PTag) {
				foundTag = ((Expression.PTag) e).tag;
				continue;
			}
			if (e instanceof Expression.PReplace) {
				foundReplace = ((Expression.PReplace) e).value;
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
				if (e instanceof Expression.PTag) {
					foundTag = ((Expression.PTag) e).tag;
					continue;
				}
				movableTag = checkTag(e, 0);
				if (!movableTag) {
					foundTag = null;
				}
			}
			if (movableReplace) {
				if (e instanceof Expression.PReplace) {
					foundReplace = ((Expression.PReplace) e).value;
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
		Expression.PTree newTree = new Expression.PTree(tree.folding, tree.label, beginShift, inner, foundTag,
				foundReplace, tree.endShift, ref(tree));
		return optimized(tree, Expression.newSequence(Expression.newSequence(factored, ref(tree)), newTree, null));
	}

	private int consumed(Expression e, int depth) {
		if (e instanceof Expression.PNot || e instanceof Expression.PEmpty) {
			return 0;
		}
		if (e instanceof Expression.PByte || e instanceof Expression.PAny || e instanceof Expression.PByteSet) {
			return 1;
		}
		if (e instanceof Expression.PNonTerminal) {
			if (depth < 10) {
				return consumed(Expression.deref(e), depth + 1);
			}
			return -1;
		}
		if (e instanceof Expression.PPair) {
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
		if (e instanceof Expression.PChoice) {
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
		if (e instanceof Expression.PNonTerminal) {
			if (depth < 10) {
				return checkTag(Expression.deref(e), depth + 1);
			}
			return false; // unchecked
		}
		if (e instanceof Expression.PTag) {
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
		if (e instanceof Expression.PNonTerminal) {
			if (depth < 10) {
				return checkTag(Expression.deref(e), depth + 1);
			}
			return false; // unchecked
		}
		if (e instanceof Expression.PReplace) {
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
	public Expression visitPair(Expression.PPair pair, Void a) {
		if (pair.get(0) instanceof Expression.PTree) {
			Expression.PTree tree = (Expression.PTree) pair.get(0);
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
			tree = new Expression.PTree(tree.folding, tree.label, tree.beginShift, Expression.newSequence(inners, null),
					tree.tag, tree.value, endShift, null);
			return optimized(pair,
					Expression.newSequence(visitTree(tree, a), Expression.newSequence(remained, null), null));
		}
		return super.visitPair(pair, a);
	}

	@Override
	public Expression visitLinkTree(Expression.PLinkTree p, Void a) {
		if (p.get(0) instanceof Expression.PChoice) {
			Expression choice = p.get(0);
			List<Expression> l = Expression.newList(choice.size());
			for (Expression inner : choice) {
				inner = rewrite(inner, a);
				l.add(new Expression.PLinkTree(p.label, inner, null));
			}
			return optimized(p, Expression.newChoice(l, null));
		}
		return super.visitLinkTree(p, a);
	}

}
