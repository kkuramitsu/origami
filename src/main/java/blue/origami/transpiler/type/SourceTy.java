package blue.origami.transpiler.type;

public class SourceTy extends BaseTy {

	private String target;

	public SourceTy(String name, String target) {
		super(name);
		this.target = target;
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.mapType(this.target);
	}

}