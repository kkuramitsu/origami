package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;

public class ASMRbset extends PegAsmInst {
	public final boolean[] bools;

	public ASMRbset(boolean[] bools, PegAsmInst next) {
		super(next);
		this.bools = bools;
		bools[0] = false;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitRSet(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		while (this.bools[px.prefetch()]) {
			px.move(1);
		}
		return this.next;
	}

}