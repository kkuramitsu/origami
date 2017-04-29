package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMSScan extends PegAsmInst {
	public final long mask;
	public final int shift;

	public ASMSScan(long mask, int shift, PegAsmInst next) {
		super(next);
		this.mask = mask;
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNScan(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int ppos = sc.xPPos();
		sc.scanCount(ppos, this.mask, this.shift);
		return this.next;
	}
}