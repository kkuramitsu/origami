package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public class ASMRbset extends PAsmInst {
	public final boolean[] bools;

	public ASMRbset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
		bools[0] = false;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRSet(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		while (this.bools[px.getbyte()]) {
			px.move(1);
		}
		return this.next;
	}

}