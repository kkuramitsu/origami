package blue.nez.parser.pasm;

public final class Inop extends PAsmInst {
	public final String name;

	public Inop(String name, PAsmInst next) {
		super(next);
		this.name = name;
	}

	@Override
	public PAsmInst exec(PAsmContext px) throws PAsmTerminationException {
		return this.next;
	}

}