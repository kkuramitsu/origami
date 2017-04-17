package blue.nez.pegasm;

import blue.nez.parser.PegAsmInstruction;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMdispatch extends PegAsmInstruction {
	public final byte[] jumpIndex;
	public final PegAsmInstruction[] jumpTable;

	public ASMdispatch(byte[] jumpIndex, PegAsmInstruction[] jumpTable) {
		super(null);
		this.jumpIndex = jumpIndex;
		this.jumpTable = jumpTable;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitDispatch(this);
	}

	@Override
	public PegAsmInstruction exec(PegAsmContext<?> sc) throws ParserTerminationException {
		int ch = sc.prefetch();
		return this.jumpTable[this.jumpIndex[ch] & 0xff];
	}

}