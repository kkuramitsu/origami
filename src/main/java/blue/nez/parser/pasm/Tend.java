package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;

public final class Tend extends PAsmInst {
	public final int shift;
	public final Symbol tag;
	public final Object value;

	public Tend(Symbol tag, Object value, int shift, PAsmInst next) {
		super(next);
		this.tag = tag;
		this.value = value;
		this.shift = shift;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		endTree(px, this.shift, this.tag, this.value);
		return this.next;
	}
}