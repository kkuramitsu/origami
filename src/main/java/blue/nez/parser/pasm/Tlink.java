package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;

public final class Tlink extends PAsmInst {
	public final Symbol label;

	public Tlink(Symbol label, PAsmInst next) {
		super(next);
		this.label = label;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		popTree(px, this.label);
		return this.next;
	}

}