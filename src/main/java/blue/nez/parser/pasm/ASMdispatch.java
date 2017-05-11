package blue.nez.parser.pasm;

import blue.nez.parser.PAsmInst;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.ParserTerminationException;

public class ASMdispatch extends PAsmInst {
	public final byte[] jumpIndex;
	public final PAsmInst[] jumpTable;

	public ASMdispatch(byte[] jumpIndex, PAsmInst[] jumpTable) {
		super(null);
		this.jumpIndex = jumpIndex;
		this.jumpTable = jumpTable;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		v.visitDispatch(this);
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		int ch = px.getbyte();
		return this.jumpTable[this.jumpIndex[ch] & 0xff];
	}

}