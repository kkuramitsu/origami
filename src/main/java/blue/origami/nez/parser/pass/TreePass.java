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

package blue.origami.nez.parser.pass;

import java.util.List;

import blue.origami.nez.ast.Symbol;
import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.expression.PAny;
import blue.origami.nez.peg.expression.PByte;
import blue.origami.nez.peg.expression.PByteSet;
import blue.origami.nez.peg.expression.PChoice;
import blue.origami.nez.peg.expression.PEmpty;
import blue.origami.nez.peg.expression.PLinkTree;
import blue.origami.nez.peg.expression.PNonTerminal;
import blue.origami.nez.peg.expression.PNot;
import blue.origami.nez.peg.expression.PPair;
import blue.origami.nez.peg.expression.PTag;
import blue.origami.nez.peg.expression.PTree;
import blue.origami.nez.peg.expression.PValue;

public class TreePass extends CommonPass {

	@Override
	public Expression visitTree(PTree tree, Void a) {
		List<Expression> l = Expression.newList(64);
		Expression.addSequence(l, this.rewrite(tree.get(0), a));
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
			if (e instanceof PValue) {
				foundReplace = ((PValue) e).value;
				continue;
			}
			int len = this.consumed(e, 0);
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
				movableTag = this.checkTag(e, 0);
				if (!movableTag) {
					foundTag = null;
				}
			}
			if (movableReplace) {
				if (e instanceof PValue) {
					foundReplace = ((PValue) e).value;
					continue;
				}
				movableReplace = this.checkReplace(e, 0);
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
		Expression inner = Expression.newSequence(inners);
		PTree newTree = new PTree(tree.folding, tree.label, beginShift, inner, foundTag, foundReplace, tree.endShift);
		return this.optimized(tree, Expression.newSequence(Expression.newSequence(factored), newTree));
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
				return this.consumed(Expression.deref(e), depth + 1);
			}
			return -1;
		}
		if (e instanceof PPair) {
			int len = this.consumed(e.get(0), depth);
			if (len == -1) {
				return -1;
			}
			int len2 = this.consumed(e.get(1), depth);
			if (len2 == -1) {
				return -1;
			}
			return len + len2;
		}
		if (e instanceof PChoice) {
			int len = this.consumed(e.get(0), depth);
			if (len == -1) {
				return -1;
			}
			for (Expression sub : e) {
				if (len != this.consumed(sub, depth)) {
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
				return this.checkTag(Expression.deref(e), depth + 1);
			}
			return false; // unchecked
		}
		if (e instanceof PTag) {
			return false;
		}
		for (Expression sub : e) {
			if (this.checkTag(sub, depth) == false) {
				return false;
			}
		}
		return true;
	}

	private boolean checkReplace(Expression e, int depth) {
		if (e instanceof PNonTerminal) {
			if (depth < 10) {
				return this.checkTag(Expression.deref(e), depth + 1);
			}
			return false; // unchecked
		}
		if (e instanceof PValue) {
			return false;
		}
		for (Expression sub : e) {
			if (this.checkReplace(sub, depth) == false) {
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
			Expression.addSequence(l, this.rewrite(pair.get(1), a));
			List<Expression> remained = Expression.newList(l.size());
			int endShift = tree.endShift;
			int i;
			for (i = 0; i < l.size(); i++) {
				Expression e = l.get(i);
				int len = this.consumed(e, 0);
				if (len == -1) {
					break;
				}
				endShift -= len;
				inners.add(e);
			}
			for (; i < l.size(); i++) {
				remained.add(l.get(i));
			}
			tree = new PTree(tree.folding, tree.label, tree.beginShift, Expression.newSequence(inners), tree.tag,
					tree.value, endShift);
			return this.optimized(pair,
					Expression.newSequence(this.visitTree(tree, a), Expression.newSequence(remained)));
		}
		return super.visitPair(pair, a);
	}

	@Override
	public Expression visitLinkTree(PLinkTree p, Void a) {
		if (p.get(0) instanceof PChoice) {
			Expression choice = p.get(0);
			List<Expression> l = Expression.newList(choice.size());
			for (Expression inner : choice) {
				inner = this.rewrite(inner, a);
				l.add(new PLinkTree(p.label, inner));
			}
			return this.optimized(p, Expression.newChoice(l));
		}
		return super.visitLinkTree(p, a);
	}

}
