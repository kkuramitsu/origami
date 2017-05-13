package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;

public final class ASMTfold extends PAsmInst {
	public final int shift;
	public final Symbol label;

	public ASMTfold(Symbol label, int shift, PAsmInst next) {
		super(next);
		this.label = label;
		this.shift = shift;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		foldTree(px, this.shift, this.label);
		return this.next;
	}
}