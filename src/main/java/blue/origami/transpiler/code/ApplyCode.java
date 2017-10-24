package blue.origami.transpiler.code;

import java.util.List;

import blue.origami.common.OArrays;
import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class ApplyCode extends CodeN {
	public ApplyCode(Code... values) {
		super(values);
	}

	public ApplyCode(List<Code> l) {
		this(l.toArray(new Code[l.size()]));
	}

	@Override
	public Code asType(Env env, Ty ret) {
		return env.getLanguage().typeApply(env, this, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushApply(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.args[0]);
		sb.append("(");
		OStrings.joins(sb, OArrays.ltrim2(this.args), ",");
		sb.append(")");
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.TypeAnnotation(this.getType(), () -> {
			this.args[0].dumpCode(sh);
			sh.Token("(");
			for (int i = 1; i < this.args.length; i++) {
				if (i > 1) {
					sh.Token(",");
				}
				sh.Expr(this.args[i]);
			}
			sh.Token(")");
		});
	}

}
