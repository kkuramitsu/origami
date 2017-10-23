package blue.origami.common;

import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;

public class SyntaxBuilder extends OConsole {
	StringBuilder sb = new StringBuilder();
	int indent = 0;

	public void Indent() {
		this.sb.append("\n");
		for (int i = 0; i < this.indent; i++) {
			this.sb.append("   ");
		}
	}

	public void incIndent() {
		this.indent++;
	}

	public void decIndent() {
		this.indent--;
	}

	public void s() {
		this.sb.append(" ");
	}

	public void append(Object t) {
		this.sb.append(t);
	}

	public void Keyword(String t) {
		this.sb.append(bold(t));
	}

	public void Name(String t) {
		this.sb.append(color(Cyan, t));
	}

	public void Token(String t) {
		this.sb.append(t);
	}

	public void Operator(String t) {
		this.sb.append(bold(t));
	}

	public void Literal(Object t) {
		this.sb.append(t);
	}

	public void StringLiteral(String t) {
		this.Literal(OStringUtils.quoteString('"', t, '"'));
	}

	public void Type(Ty t) {
		this.sb.append(color(Red, t.finalTy().toString()));
	}

	private boolean untyped = false;

	public void NoAnno(Runnable r) {
		boolean stack = this.untyped;
		this.untyped = true;
		r.run();
		this.untyped = stack;
	}

	public void TypeAnnotation(Ty ty, Runnable r) {
		if (this.untyped) {
			r.run();
		} else {
			this.sb.append(color(Red, "("));
			r.run();
			this.sb.append(color(Red, " :" + ty.finalTy()));
			this.sb.append(color(Red, ")"));
		}
	}

	public void TypeAnnotation_(Ty ty, Runnable r) {
		r.run();
		if (!this.untyped) {
			this.sb.append(color(Red, " :" + ty.finalTy()));
		}
	}

	public void Expr(Code c) {
		c.dumpCode(this);
	}

	public void Error(TLog log) {
		this.sb.append("<ERROR>");
	}

	@Override
	public String toString() {
		return this.sb.toString();
	}

}
