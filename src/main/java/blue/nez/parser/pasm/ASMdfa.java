package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public final class ASMdfa extends PAsmInst {
	public final byte[] jumpIndex;
	public final PAsmInst[] jumpTable;

	public ASMdfa(byte[] jumpIndex, PAsmInst[] jumpTable) {
		super(null);
		this.jumpIndex = jumpIndex;
		this.jumpTable = jumpTable;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitDDispatch(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		return this.jumpTable[this.jumpIndex[px.nextbyte()] & 0xff];
	}

}