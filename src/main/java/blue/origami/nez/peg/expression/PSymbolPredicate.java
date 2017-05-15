package blue.origami.nez.peg.expression;

import blue.origami.nez.ast.Symbol;
import blue.origami.nez.parser.pasm.PAsmAPI.SymbolFunc;
import blue.origami.nez.peg.Expression;
import blue.origami.nez.peg.ExpressionVisitor;

public class PSymbolPredicate extends PUnary {
	public final boolean andPred;
	public final SymbolFunc pred;
	public final Symbol label;

	public PSymbolPredicate(SymbolFunc pred, boolean andPred, Symbol label, PNonTerminal pat) {
		super(pat == null ? Expression.defaultEmpty : pat);
		this.andPred = andPred;
		this.pred = pred;
		this.label = label;
	}

	public PSymbolPredicate(SymbolFunc pred, boolean andPred, String label, PNonTerminal pat) {
		this(pred, andPred, Symbol.unique(label), pat);
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitSymbolPredicate(this, a);
	}

	public String getFunctionName() {
		return this.pred.toString().replace("&", "");
	}

	public boolean isAndPredicate() {
		return this.andPred;
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("<");
		sb.append(this.getFunctionName());
		sb.append(" ");
		sb.append(this.label);
		// if (!this.isEmpty()) {
		// sb.append(" ");
		// this.get(0).strOut(sb);
		// }
		sb.append(">");
	}

}