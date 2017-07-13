package blue.origami.transpiler;

public class TType {
	public static final TType tUntyped = new TSimpleType("?");
	public static final TType tUnit = new TSimpleType("Unit");
	public static final TType tBool = new TSimpleType("Bool");
	public static final TType tInt = new TSimpleType("Int");
	public static final TType tString = new TSimpleType("String");

	//

}

class TSimpleType extends TType {
	private String name;

	TSimpleType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}
}