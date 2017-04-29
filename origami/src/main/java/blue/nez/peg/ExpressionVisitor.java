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

package blue.nez.peg;

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
import blue.nez.peg.expression.PValue;
import blue.nez.peg.expression.PScan;
import blue.nez.peg.expression.PSymbolAction;
import blue.nez.peg.expression.PSymbolPredicate;
import blue.nez.peg.expression.PSymbolScope;
import blue.nez.peg.expression.PTag;
import blue.nez.peg.expression.PTrap;
import blue.nez.peg.expression.PTree;

public abstract class ExpressionVisitor<V, A> {

	public abstract V visitTrap(PTrap e, A a);

	public abstract V visitNonTerminal(PNonTerminal e, A a);

	public abstract V visitEmpty(PEmpty e, A a);

	public abstract V visitFail(PFail e, A a);

	public abstract V visitByte(PByte e, A a);

	public abstract V visitByteSet(PByteSet e, A a);

	public abstract V visitAny(PAny e, A a);

	public abstract V visitPair(PPair e, A a);

	public abstract V visitChoice(PChoice e, A a);

	public abstract V visitDispatch(PDispatch e, A a);

	public abstract V visitOption(POption e, A a);

	public abstract V visitRepetition(PRepetition e, A a);

	public abstract V visitAnd(PAnd e, A a);

	public abstract V visitNot(PNot e, A a);

	public abstract V visitTree(PTree e, A a);

	public abstract V visitDetree(PDetree e, A a);

	public abstract V visitLinkTree(PLinkTree e, A a);

	public abstract V visitTag(PTag e, A a);

	public abstract V visitReplace(PValue e, A a);

	public abstract V visitSymbolScope(PSymbolScope e, A a);

	public abstract V visitSymbolAction(PSymbolAction e, A a);

	public abstract V visitSymbolPredicate(PSymbolPredicate e, A a);

	public abstract V visitIf(PIfCondition e, A a);

	public abstract V visitOn(POnCondition e, A a);

	public abstract V visitScan(PScan scanf, A a);

	public abstract V visitRepeat(PRepeat e, A a);

	public V visitLocal(Expression e, A a) {
		return e.desugar().visit(this, a);
	}
}
