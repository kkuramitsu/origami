package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;

public class ASMObset extends PegAsmInst {
	public final boolean[] bools;

	public ASMObset(boolean[] bools, PegAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitOSet(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		int byteChar = px.prefetch();
		if (this.bools[byteChar]) {
			px.move(1);
		}
		return this.next;
	}

}