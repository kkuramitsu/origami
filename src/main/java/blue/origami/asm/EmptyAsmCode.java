package blue.origami.asm;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.type.Ty;

public class EmptyAsmCode implements Code {

	Ty type;

	EmptyAsmCode(Ty type) {
		this.type = type;
	}

	@Override
	public Ty getType() {
		return this.type;
	}

	@Override
	public void emitCode(TEnv env, CodeSection sec) {

	}

	@Override
	public void strOut(StringBuilder sb) {
	}

	@Override
	public Code setSource(AST t) {
		return this;
	}

	@Override
	public AST getSource() {
		return null;
	}

	@Override
	public Code[] args() {
		return TArrays.emptyCodes;
	}

}
