package blue.origami.transpiler.type;

import java.util.function.Predicate;

import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;

public class VarParamTy extends BaseTy {
	public VarParamTy(String name) {
		super(name, 1);
	}

	@Override
	public Ty newGeneric(Ty paramTy) {
		return new GenericTy(this, paramTy);
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return f.test(this);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		return dom.convToVar(this);
	}

	public String getId() {
		return this.name;
	}

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return codeType.mapType("a");
	}

	@Override
	public int costMapThisTo(Env env, Ty fromTy, Ty toTy) {
		return CodeMap.BESTCAST;
	}

	@Override
	public CodeMap findMapThisTo(Env env, Ty fromTy, Ty toTy) {
		return new CodeMap(CodeMap.BESTCAST | CodeMap.LazyFormat, "anycast", "anycast", this, toTy);
	}

	@Override
	public int costMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return CodeMap.BESTCAST;
	}

	@Override
	public CodeMap findMapFromToThis(Env env, Ty fromTy, Ty toTy) {
		return new CodeMap(CodeMap.BESTCAST | CodeMap.LazyFormat, "upcast", "upcast", fromTy, this);
	}

}