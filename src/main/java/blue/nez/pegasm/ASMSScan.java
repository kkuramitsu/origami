package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMSScan extends PegAsmInstruction {
	public final long mask;
	public final int shift;

	public ASMSScan(long mask, int shift, PegAsmInstruction next) {
		super(next);
		this.mask = mask;
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNScan(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int ppos = sc.xPPos();
		sc.scanCount(ppos, this.mask, this.shift);
		return this.next;
	}
}