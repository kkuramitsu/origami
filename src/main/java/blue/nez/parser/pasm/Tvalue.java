package blue.nez.parser.pasm;

public final class Tvalue extends PAsmInst {
	public final Object value;

	public Tvalue(Object value, PAsmInst next) {
		super(next);
		this.value = value;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		valueTree(px, this.value);
		return this.next;
	}

}