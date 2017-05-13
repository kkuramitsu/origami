package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;

public final class ASMTemit extends PAsmInst {
	public final Symbol label;

	public ASMTemit(Symbol label, PAsmInst next) {
		super(next);
		this.label = label;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return this.next;
	}

}