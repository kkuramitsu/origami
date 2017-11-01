package blue.origami.transpiler.type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import blue.origami.common.OStrings;

public class TagTy extends Ty {
	protected String[] tags;
	protected Ty innerTy;

	public TagTy(Ty ty, String... names) {
		this.tags = names;
		this.innerTy = ty;
	}

	@Override
	public Ty getParamType() {
		return this.innerTy;
	}

	@Override
	public boolean isMutable() {
		return this.innerTy.isMutable();
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return this.innerTy.hasSome(f);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		Ty inner = this.innerTy.dupVar(dom);
		if (inner != this.innerTy) {
			return Ty.tTag(inner, this.tags);
		}
		return this;
	}

	@Override
	public Ty map(Function<Ty, Ty> f) {
		Ty self = f.apply(this);
		if (self != this) {
			return self;
		}
		return Ty.tTag(this.innerTy.map(f), this.tags);
	}

	@Override
	public Ty memoed() {
		if (!this.isMemoed()) {
			return Ty.tTag(this.innerTy.memoed(), this.tags);
		}
		return this;
	}

	@Override
	public boolean acceptTy(boolean sub, Ty codeTy, VarLogger logs) {
		if (codeTy instanceof TagTy && this.innerTy.acceptTy(sub, codeTy.getParamType(), logs)) {
			return this.matchTags(sub, ((TagTy) codeTy).tags);
		}
		return this.acceptVarTy(sub, codeTy, logs);
	}

	public boolean matchTags(boolean sub, String[] names) {
		if (this.tags.length != names.length) {
			return false;
		}
		for (int i = 0; i < names.length; i++) {
			if (!this.tags[i].equals(names[i])) {
				return false;
			}
		}
		return true;
	}
	//
	// @Override
	// public int costMapTo(TEnv env, Ty toTy) {
	// if(toTy.acceptTy(false, this.innerTy, logs)) {
	// return
	// }
	// return CastCode.STUPID;
	// }
	//
	// @Override
	// public Template findMapTo(TEnv env, Ty toTy) {
	// return null;
	// }

	// @Override
	// public String key() {
	// return this.name + "[" + this.innerTy.key() + "]";
	// }

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return this.innerTy.mapType(codeType);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.innerTy);
		sb.append(" #");
		OStrings.joins(sb, this.tags, " #");
	}

	public static String[] joins(String[] ns, String[] names) {
		if (ns.length == 1) {
			if (Arrays.stream(names).anyMatch(n -> n.equals(ns[0]))) {
				return names;
			}
			return OArrays.join(String[]::new, ns[0], names);
		}
		if (names.length == 1) {
			return joins(names, ns);
		}
		HashSet<String> set = new HashSet<>();
		Arrays.stream(ns).forEach(n -> {
			set.add(n);
		});
		Arrays.stream(names).forEach(n -> {
			set.add(n);
		});
		return set.toArray(new String[set.size()]);
	}

}
