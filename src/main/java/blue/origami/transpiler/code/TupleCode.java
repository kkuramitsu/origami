package blue.origami.transpiler.code;

import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.OStrings;

public class TupleCode extends CodeN {

	public TupleCode(Code[] values) {
		super(values);
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			Ty[] ts = new Ty[this.args.length];
			for (int i = 0; i < this.args.length; i++) {
				ts[i] = this.asTypeAt(env, i, Ty.tUntyped());
			}
			this.setType(Ty.tTuple(ts));
		}
		// if(ret.isTuple(this.args.length)) {
		//
		// }
		return super.castType(env, ret);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushTuple(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		OStrings.joins(sb, this.args, ",");
		sb.append(")");
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.Token("(");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 0) {
				sh.Token(",");
			}
			sh.Expr(this.args[i]);
		}
		sh.Token(")");
	}

}