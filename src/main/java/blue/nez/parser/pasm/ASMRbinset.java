package blue.nez.parser.pasm;

import blue.nez.parser.ParserTerminationException;
import blue.nez.parser.PAsmContext;
import blue.nez.parser.PAsmInst;

public class ASMRbinset extends PAsmInst {
	public final boolean[] bools;

	public ASMRbinset(boolean[] bools, PAsmInst next) {
		super(next);
		this.bools = bools;
	}

	@Override
	public PAsmInst exec(PAsmContext<?> px) throws ParserTerminationException {
		while (this.bools[px.getbyte()] && !px.eof()) {
			px.move(1);
		}
		return this.next;
	}

	@Override
	public void visit(PegAsmVisitor v) {
		// TODO Auto-generated method stub

	}

}