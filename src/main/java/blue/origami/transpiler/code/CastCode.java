package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class CastCode extends Code1 implements MappedCode {
	public CastCode(Ty ret, CodeMap tp, Code inner) {
		super(ret, inner);
		this.setTemplate(tp);
		// ODebug.trace("CAST *****(%s => %s) %s", inner.getType(), ret, inner);
	}

	public CastCode(Ty ret, Code inner) {
		this(ret, null, inner);
	}

	private CodeMap tp;

	public void setTemplate(CodeMap tp) {
		this.tp = tp;
	}

	@Override
	public CodeMap getMapped() {
		return this.tp;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.tp == null) {
			Code in = this.getInner().asType(env, this.getType());
			// ODebug.trace("casting %s %s %s => %s => %s",
			// in.getClass().getSimpleName(), in, in.getType(),
			// this.getType(), ret);
			return in.castType(env, ret);
		}
		return this.castType(env, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushCast(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "cast-to-" + this.getType(), this.getInner());
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Token("(");
		sh.Type(this.getType());
		sh.Token(")");
		sh.Expr(this.getInner());
	}

	public static class MutableCode extends CastCode {

		public MutableCode(Code inner) {
			super(null, inner);
		}

	}

	public static class BoxCastCode extends CastCode {

		public BoxCastCode(Ty ret, Code inner) {
			super(ret, null, inner);
		}

	}

	public static class UnboxCastCode extends CastCode {

		public UnboxCastCode(Ty ret, Code inner) {
			super(ret, null, inner);
		}

	}

	public static class FuncCastCode extends CastCode {

		public FuncCastCode(Ty ty, CodeMap tp, Code inner) {
			super(ty, tp, inner);
		}

	}

}
