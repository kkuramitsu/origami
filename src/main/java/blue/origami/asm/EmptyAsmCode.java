package blue.origami.asm;

import blue.origami.common.OArrays;
import origami.nez2.Token;
import blue.origami.transpiler.CodeSection;
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
	public void emitCode(CodeSection sec) {

	}

	@Override
	public void strOut(StringBuilder sb) {
	}

	@Override
	public Code setSource(Token s) {
		return this;
	}

	@Override
	public Token getSource() {
		return null;
	}

	@Override
	public Code[] args() {
		return OArrays.emptyCodes;
	}

}
