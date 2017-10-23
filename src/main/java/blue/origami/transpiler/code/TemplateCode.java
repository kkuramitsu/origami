package blue.origami.transpiler.code;

import blue.origami.common.OStrings;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class TemplateCode extends CodeN {

	public TemplateCode(Code... codes) {
		super(Ty.tString, codes);
	}

	@Override
	public void emitCode(Env env, CodeSection sec) {
		sec.pushTemplate(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 1) {
				sb.append("+");
			}
			OStrings.append(sb, this.args[i]);
		}
		sb.append(")");
	}

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.append("(");
		for (int i = 0; i < this.args.length; i++) {
			if (i > 1) {
				sh.append("++");
			}
			this.args[i].dumpCode(sh);
		}
		sh.append(")");
	}

}