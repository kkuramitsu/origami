package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.type.Ty;
import origami.nez2.Token;

public class NoneCode extends CommonCode implements ValueCode {

	public NoneCode(Token s) {
		super(Ty.tOption(Ty.tVar(s)));
		this.setSource(s);
	}

	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushNone(this);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.TypeAnnotation_(this.getType(), () -> {
			sh.Keyword("None");
		});
	}

}