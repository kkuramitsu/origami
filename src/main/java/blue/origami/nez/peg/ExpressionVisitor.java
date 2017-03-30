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

public abstract class ExpressionVisitor<V, A> {

	public abstract V visitTrap(Expression.PTrap e, A a);

	public abstract V visitNonTerminal(Expression.PNonTerminal e, A a);

	public abstract V visitEmpty(Expression.PEmpty e, A a);

	public abstract V visitFail(Expression.PFail e, A a);

	public abstract V visitByte(Expression.PByte e, A a);

	public abstract V visitByteSet(Expression.PByteSet e, A a);

	public abstract V visitAny(Expression.PAny e, A a);

	public abstract V visitPair(Expression.PPair e, A a);

	public abstract V visitChoice(Expression.PChoice e, A a);

	public abstract V visitDispatch(Expression.PDispatch e, A a);

	public abstract V visitOption(Expression.POption e, A a);

	public abstract V visitRepetition(Expression.PRepetition e, A a);

	public abstract V visitAnd(Expression.PAnd e, A a);

	public abstract V visitNot(Expression.PNot e, A a);

	public abstract V visitTree(Expression.PTree e, A a);

	public abstract V visitDetree(Expression.PDetree e, A a);

	public abstract V visitLinkTree(Expression.PLinkTree e, A a);

	public abstract V visitTag(Expression.PTag e, A a);

	public abstract V visitReplace(Expression.PReplace e, A a);

	public abstract V visitSymbolScope(Expression.PSymbolScope e, A a);

	public abstract V visitSymbolAction(Expression.PSymbolAction e, A a);

	public abstract V visitSymbolPredicate(Expression.PSymbolPredicate e, A a);

	public abstract V visitIf(Expression.PIfCondition e, A a);

	public abstract V visitOn(Expression.POnCondition e, A a);

	public abstract V visitScan(Expression.PScan scanf, A a);

	public abstract V visitRepeat(Expression.PRepeat e, A a);

	public V visitLocal(Expression e, A a) {
		return e.desugar().visit(this, a);
	}
}
