package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMSScan extends PAsmInst {
	public final long mask;
	public final int shift;

	public ASMSScan(long mask, int shift, PAsmInst next) {
		super(next);
		this.mask = mask;
		this.shift = shift;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitNScan(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> sc) throws ParserTerminationException {
		int ppos = sc.xPPos();
		sc.scanCount(ppos, this.mask, this.shift);
		return this.next;
	}
}