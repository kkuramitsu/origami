package blue.origami.transpiler.type;

import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.CastCode;

class AnyTy extends SimpleTy {

	AnyTy() {
		super("AnyRef");
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		return codeTy == this;
	}

	@Override
	public int costMapTo(Env env, Ty toTy) {
		return CastCode.BESTCAST;
	}

	@Override
	public CodeMap findMapTo(Env env, Ty toTy) {
		return new CodeMap(CastCode.BESTCAST | CodeMap.LazyFormat, "anycast", "anycast", this, toTy);
	}

	@Override
	public int costMapFrom(Env env, Ty fromTy) {
		return CastCode.BESTCAST;
	}

	@Override
	public CodeMap findMapFrom(Env env, Ty fromTy) {
		return new CodeMap(CastCode.BESTCAST | CodeMap.LazyFormat, "upcast", "upcast", fromTy, this);
	}

}