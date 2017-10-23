package blue.origami.parser.peg;

import blue.origami.nez.ast.Symbol;

public class PLinkTree extends PUnary {
	public Symbol label;

	public PLinkTree(Symbol label, Expression e) {
		super(e);
		this.label = label;
	}

	@Override
	protected Object[] extract() {
		return new Object[] { this.label };
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitLinkTree(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.formatUnary((this.label != null) ? "$" + this.label + "(" : "$(", ")", sb);
	}
}