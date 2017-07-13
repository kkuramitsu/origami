package blue.origami.transpiler;

public class TType {
	public static final TType tUntyped = new TSimpleType("?");
	public static final TType tUnit = new TSimpleType("Unit");
	public static final TType tBool = new TSimpleType("Bool");
	public static final TType tInt = new TSimpleType("Int");
	public static final TType tFloat = new TSimpleType("Float");
	public static final TType tString = new TSimpleType("String");
	public static final TType tData = new TSimpleType("Data");

	//

	@Override
	public boolean equals(Object t) {
		return this == t;
	}

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