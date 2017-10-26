package blue.origami.transpiler.code;

import blue.origami.transpiler.CodeBuilder;
import blue.origami.transpiler.CodeSection;

public class SwitchCode extends CodeN implements CodeBuilder {

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushSwitch(this);
	}

}