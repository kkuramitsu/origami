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

package blue.origami.nezcc;

import blue.nez.ast.Symbol;
import blue.nez.parser.ParserContext;
import blue.nez.parser.ParserContext.SymbolDefinition;
import blue.nez.parser.ParserContext.SymbolExist;
import blue.nez.parser.ParserContext.SymbolReset;
import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.expression.PAnd;
import blue.nez.peg.expression.PAny;
import blue.nez.peg.expression.PByte;
import blue.nez.peg.expression.PByteSet;
import blue.nez.peg.expression.PChoice;
import blue.nez.peg.expression.PDetree;
import blue.nez.peg.expression.PDispatch;
import blue.nez.peg.expression.PEmpty;
import blue.nez.peg.expression.PFail;
import blue.nez.peg.expression.PIfCondition;
import blue.nez.peg.expression.PLinkTree;
import blue.nez.peg.expression.PNonTerminal;
import blue.nez.peg.expression.PNot;
import blue.nez.peg.expression.POnCondition;
import blue.nez.peg.expression.POption;
import blue.nez.peg.expression.PPair;
import blue.nez.peg.expression.PRepeat;
import blue.nez.peg.expression.PRepetition;
import blue.nez.peg.expression.PReplace;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;

public class ParserInterpreter<T> extends ExpressionVisitor<Boolean, ParserContext<T>> {

	public Boolean parse(Expression e, ParserContext<T> px) {
		return e.visit(this, px);
	}

	@Override
	public Boolean visitNonTerminal(PNonTerminal e, ParserContext<T> px) {
		return this.parse(e.getExpression(), px);
	}

	@Override
	public Boolean visitEmpty(PEmpty e, ParserContext<T> px) {
		return true;
	}

	@Override
	public Boolean visitFail(PFail e, ParserContext<T> px) {
		return false;
	}

	@Override
	public Boolean visitByte(PByte e, ParserContext<T> px) {
		return e.byteSet().is(px.read());
	}

	@Override
	public Boolean visitByteSet(PByteSet e, ParserContext<T> px) {
		return e.byteSet().is(px.read());
	}

	@Override
	public Boolean visitAny(PAny e, ParserContext<T> px) {
		return !(px.eof());
	}

	@Override
	public Boolean visitPair(PPair e, ParserContext<T> px) {
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		return this.parse(e.get(1), px);
	}

	@Override
	public Boolean visitChoice(PChoice e, ParserContext<T> px) {
		int pos = px.pos;
		T node = px.loadTree();
		int treeLog = px.loadTreeLog();
		Object state = px.loadSymbolTable();
		for (Expression sub : e) {
			px.backtrack(pos);
			px.storeTree(node);
			px.storeTreeLog(treeLog);
			px.storeSymbolTable(state);
			if (this.parse(sub, px)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Boolean visitDispatch(PDispatch e, ParserContext<T> px) {
		int u = px.prefetch();
		return this.parse(e.get(e.indexMap[u] & 0xff), px);
	}

	@Override
	public Boolean visitOption(POption e, ParserContext<T> px) {
		int pos = px.pos;
		T node = px.loadTree();
		int treeLog = px.loadTreeLog();
		Object state = px.loadSymbolTable();
		if (!this.parse(e.get(0), px)) {
			px.backtrack(pos);
			px.storeTree(node);
			px.storeTreeLog(treeLog);
			px.storeSymbolTable(state);
		}
		return true;
	}

	@Override
	public Boolean visitRepetition(PRepetition e, ParserContext<T> px) {
		int pos = px.pos;
		T node = px.loadTree();
		int treeLog = px.loadTreeLog();
		Object state = px.loadSymbolTable();
		int c = 0;
		for (; this.parse(e.get(0), px); c++) {
			if (pos == px.pos) {
				break;
			}
			pos = px.pos;
			node = px.loadTree();
			treeLog = px.loadTreeLog();
			state = px.loadSymbolTable();
		}
		if (c > e.min) {
			px.backtrack(pos);
			px.left = node;
			px.storeTree(node);
			px.storeTreeLog(treeLog);
			px.storeSymbolTable(state);
			return true;
		}
		return false;
	}

	@Override
	public Boolean visitAnd(PAnd e, ParserContext<T> px) {
		int pos = px.pos;
		T node = px.loadTree();
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		px.backtrack(pos);
		px.storeTree(node);
		return true;
	}

	@Override
	public Boolean visitNot(PNot e, ParserContext<T> px) {
		int pos = px.pos;
		T node = px.loadTree();
		int treeLog = px.loadTreeLog();
		Object state = px.loadSymbolTable();
		if (!this.parse(e.get(0), px)) {
			px.backtrack(pos);
			px.storeTree(node);
			px.storeTreeLog(treeLog);
			px.storeSymbolTable(state);
			return true;
		}
		return false;
	}

	@Override
	public Boolean visitTree(PTree e, ParserContext<T> px) {
		if (e.folding) {
			px.foldTree(e.beginShift, e.label);
		} else {
			px.beginTree(e.beginShift);
		}
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		px.endTree(e.endShift, e.tag, e.value);
		return true;
	}

	@Override
	public Boolean visitDetree(PDetree e, ParserContext<T> px) {
		T node = px.loadTree();
		int treeLog = px.loadTreeLog();
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		px.storeTreeLog(treeLog);
		px.storeTree(node);
		return true;
	}

	@Override
	public Boolean visitLinkTree(PLinkTree e, ParserContext<T> px) {
		T node = px.loadTree();
		int treeLog = px.loadTreeLog();
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		px.storeTreeLog(treeLog);
		px.linkTree(node, e.label);
		px.storeTree(node);
		return true;
	}

	@Override
	public Boolean visitTag(PTag e, ParserContext<T> px) {
		px.tagTree(e.tag);
		return true;
	}

	@Override
	public Boolean visitReplace(PReplace e, ParserContext<T> px) {
		px.valueTree(e.value);
		return true;
	}

	@Override
	public Boolean visitSymbolScope(PSymbolScope e, ParserContext<T> px) {
		Object state = px.loadSymbolTable();
		if (e.label != null) { // localScope
			new SymbolReset().mutate(px, e.label, 0, null);
		}
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		px.storeSymbolTable(state);
		return true;
	}

	@Override
	public Boolean visitSymbolAction(PSymbolAction e, ParserContext<T> px) {
		int ppos = px.pos;
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		e.action.mutate(px, e.label, ppos, e.thunk);
		return true;
	}

	@Override
	public Boolean visitSymbolPredicate(PSymbolPredicate e, ParserContext<T> px) {
		int ppos = px.pos;
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		e.pred.match(px, e.label, ppos, e.thunk);
		return true;
	}

	@Override
	public Boolean visitIf(PIfCondition e, ParserContext<T> px) {
		Symbol label = Symbol.unique(e.flagName());
		boolean b = new SymbolExist().match(px, label, px.pos, null);
		return e.isPositive() ? b : !b;
	}

	@Override
	public Boolean visitOn(POnCondition e, ParserContext<T> px) {
		Symbol label = Symbol.unique(e.flagName());
		Object state = px.loadSymbolTable();
		if (e.isPositive()) {
			new SymbolDefinition().mutate(px, label, px.pos, null);
		} else {
			new SymbolReset().mutate(px, label, px.pos, null);
		}
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		px.storeSymbolTable(state);
		return true;
	}

	@Override
	public Boolean visitScan(PScan e, ParserContext<T> px) {
		int ppos = px.pos;
		if (!this.parse(e.get(0), px)) {
			return false;
		}
		px.scanCount(ppos, e.mask, e.shift);
		return true;
	}

	@Override
	public Boolean visitRepeat(PRepeat e, ParserContext<T> px) {
		// int ppos = px.pos;
		while (this.parse(e.get(0), px)) {
			if (!px.decCount()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Boolean visitTrap(PTrap e, ParserContext<T> px) {
		return true;
	}

}
