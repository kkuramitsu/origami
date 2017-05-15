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

package blue.origami.nez.peg;

import blue.origami.nez.peg.expression.PAnd;
import blue.origami.nez.peg.expression.PAny;
import blue.origami.nez.peg.expression.PByte;
import blue.origami.nez.peg.expression.PByteSet;
import blue.origami.nez.peg.expression.PChoice;
import blue.origami.nez.peg.expression.PDetree;
import blue.origami.nez.peg.expression.PDispatch;
import blue.origami.nez.peg.expression.PEmpty;
import blue.origami.nez.peg.expression.PFail;
import blue.origami.nez.peg.expression.PIf;
import blue.origami.nez.peg.expression.PLinkTree;
import blue.origami.nez.peg.expression.PMany;
import blue.origami.nez.peg.expression.PNonTerminal;
import blue.origami.nez.peg.expression.PNot;
import blue.origami.nez.peg.expression.POn;
import blue.origami.nez.peg.expression.POption;
import blue.origami.nez.peg.expression.PPair;
import blue.origami.nez.peg.expression.PSymbolAction;
import blue.origami.nez.peg.expression.PSymbolPredicate;
import blue.origami.nez.peg.expression.PSymbolScope;
import blue.origami.nez.peg.expression.PTag;
import blue.origami.nez.peg.expression.PTrap;
import blue.origami.nez.peg.expression.PTree;
import blue.origami.nez.peg.expression.PValue;

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

	public abstract V visitMany(PMany e, A a);

	public abstract V visitAnd(PAnd e, A a);

	public abstract V visitNot(PNot e, A a);

	public abstract V visitTree(PTree e, A a);

	public abstract V visitDetree(PDetree e, A a);

	public abstract V visitLinkTree(PLinkTree e, A a);

	public abstract V visitTag(PTag e, A a);

	public abstract V visitValue(PValue e, A a);

	public abstract V visitSymbolScope(PSymbolScope e, A a);

	public abstract V visitSymbolAction(PSymbolAction e, A a);

	public abstract V visitSymbolPredicate(PSymbolPredicate e, A a);

	public abstract V visitIf(PIf e, A a);

	public abstract V visitOn(POn e, A a);

	// public abstract V visitScan(PScan scanf, A a);
	//
	// public abstract V visitRepeat(PRepeat e, A a);

	public V visitLocal(Expression e, A a) {
		return e.desugar().visit(this, a);
	}
}
