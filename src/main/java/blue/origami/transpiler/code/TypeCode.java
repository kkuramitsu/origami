package blue.origami.transpiler.code;

import blue.origami.common.ODebug;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.type.Ty;
import origami.libnez.OStrings;

public class TypeCode extends CommonCode {
	private Ty value;

	public TypeCode(Ty value) {
		super(Ty.tVoid);
		this.value = value;
	}

	public Ty getTypeValue() {
		return this.value;
	}

	@Override
	public void emitCode(CodeSection sec) {
		ODebug.TODO(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.value);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Type(this.value);
	}

}
