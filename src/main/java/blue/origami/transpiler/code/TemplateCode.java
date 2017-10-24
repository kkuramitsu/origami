package blue.origami.transpiler.code;

import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.type.Ty;

public class TemplateCode extends CodeN {

	public TemplateCode(Code... codes) {
		super(Ty.tString, codes);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushTemplate(this);
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
	public void dumpCode(SyntaxBuilder sh) {
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