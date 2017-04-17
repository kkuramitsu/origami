package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public final class ASMdfa extends ASMdispatch {
	public ASMdfa(byte[] jumpIndex, PegAsmInstruction[] jumpTable) {
		super(jumpIndex, jumpTable);
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitDDispatch(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		return this.jumpTable[this.jumpIndex[sc.read()] & 0xff];
	}

}