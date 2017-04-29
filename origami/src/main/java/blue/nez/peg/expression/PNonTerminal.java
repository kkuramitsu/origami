package blue.nez.peg.expression;

import blue.nez.peg.Expression;
import blue.nez.peg.ExpressionVisitor;
import blue.nez.peg.Grammar;
import blue.nez.peg.Production;

public final class PNonTerminal extends Expression {
	// public boolean isLeftRecursion;
	private final Grammar grammar;
	final String ns;
	private final String name;

	public PNonTerminal(Grammar g, String ns, String name) {
		// this.isLeftRecursion = isLeftRecursion;
		this.grammar = g;
		this.ns = ns;
		this.name = name;
	}

	public PNonTerminal(Grammar g, String pname) {
		this(g, null, pname);
	}

	@Override
	public final boolean equals(Object o) {
		if (o instanceof PNonTerminal) {
			return this.name.equals(((PNonTerminal) o).name) && this.grammar == ((PNonTerminal) o).getGrammar();
		}
		return false;
	}

	public final Grammar getGrammar() {
		if (this.ns != null) {
			return this.grammar.getGrammar(this.ns);
		}
		return this.grammar;
	}

	public final String getLocalName() {
		return this.name;
	}

	public final String getUniqueName() {
		return this.getGrammar().getUniqueName(this.name);
	}

	public final Expression getExpression() {
		return this.getGrammar().getExpression(this.name);
	}

	public final Production getProduction() {
		return this.getGrammar().getProduction(this.name);
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Expression get(int index) {
		return null;
	}

	@Override
	public final <V, A> V visit(ExpressionVisitor<V, A> v, A a) {
		return v.visitNonTerminal(this, a);
	}

	@Override
	public void strOut(StringBuilder sb) {
		if (this.ns != null) {
			sb.append(this.ns);
			sb.append("::");
		}
		sb.append(this.name);
	}

	public String getNameSpace() {
		return this.ns;
	}
}