package blue.nez.parser.pegasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.PegAsmInst;

public final class ASMdfa extends PegAsmInst {
	public final byte[] jumpIndex;
	public final PegAsmInst[] jumpTable;

	public ASMdfa(byte[] jumpIndex, PegAsmInst[] jumpTable) {
		super(null);
		this.jumpIndex = jumpIndex;
		this.jumpTable = jumpTable;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitDDispatch(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		return this.jumpTable[this.jumpIndex[px.read()] & 0xff];
	}

}