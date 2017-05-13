package blue.nez.parser.pasm;

import blue.nez.ast.Symbol;

public final class ASMTtag extends PAsmInst {
	public final Symbol tag;

	public ASMTtag(Symbol tag, PAsmInst next) {
		super(next);
		this.tag = tag;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		tagTree(px, this.tag);
		return this.next;
	}

}