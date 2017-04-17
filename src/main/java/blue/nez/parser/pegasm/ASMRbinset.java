package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;

public class ASMRbinset extends PegAsmInst {
	public final boolean[] bools;

	public ASMRbinset(boolean[] bools, PegAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		while (this.bools[px.prefetch()] && !px.eof()) {
			px.move(1);
		}
		return this.next;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		// TODO Auto-generated method stub

	}

}