package blue.nez.parser.pegasm;

import blue.nez.parser.PegAsmInst;
import blue.nez.parser.PegAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMdispatch extends PegAsmInst {
	public final byte[] jumpIndex;
	public final PegAsmInst[] jumpTable;

	public ASMdispatch(byte[] jumpIndex, PegAsmInst[] jumpTable) {
		super(null);
		this.jumpIndex = jumpIndex;
		this.jumpTable = jumpTable;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitDispatch(this);
	}

	@Override
	public PegAsmInst exec(PegAsmContext<?> px) throws ParserTerminationException {
		int ch = px.prefetch();
		return this.jumpTable[this.jumpIndex[ch] & 0xff];
	}

}