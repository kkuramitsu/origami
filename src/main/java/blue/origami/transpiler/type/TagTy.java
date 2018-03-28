package blue.origami.transpiler.type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import origami.libnez.OStrings;

public class TagTy extends Ty {
	protected Ty baseTy;
	protected String[] tags;

	public TagTy(Ty ty, String... tags) {
		this.baseTy = ty;
		this.tags = tags;
	}

	@Override
	public boolean eq(Ty ty) {
		Ty right = ty.devar();
		if (this == right) {
			return true;
		}
		if (right instanceof TagTy) {
			TagTy dt = (TagTy) right;
			if (dt.tags.length == this.tags.length) {
				for (String tag : this.tags) {
					if (!dt.hasTag(tag)) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public Ty getParamType() {
		return this.baseTy;
	}

	@Override
	public boolean hasSome(Predicate<Ty> f) {
		return this.baseTy.hasSome(f);
	}

	@Override
	public Ty dupVar(VarDomain dom) {
		Ty inner = this.baseTy.dupVar(dom);
		if (inner != this.baseTy) {
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
		return Ty.tTag(this.baseTy.map(f), this.tags);
	}

	@Override
	public Ty memoed() {
		if (!this.isMemoed()) {
			return Ty.tTag(this.baseTy.memoed(), this.tags);
		}
		return this;
	}

	@Override
	public boolean hasSuperType(Ty left0) {
		if (left0 instanceof TagTy) {
			TagTy left = (TagTy) left0;
			for (String tag : left.tags) {
				if (!this.hasTag(tag)) {
					return false;
				}
			}
			return this.baseTy.hasSuperType(left.baseTy);
		}
		return this.baseTy.hasSuperType(left0);
	}

	private boolean hasTag(String tag) {
		for (String t : this.tags) {
			if (tag.equals(t)) {
				return true;
			}
		}
		return false;
	}

	// @Override
	// public boolean match(TypeMatchContext logs, boolean sub, Ty codeTy) {
	// if (codeTy instanceof TagTy && this.baseTy.match(logs, sub,
	// codeTy.getParamType())) {
	// return this.matchTags(sub, ((TagTy) codeTy).tags);
	// }
	// return this.matchVar(sub, codeTy, logs);
	// }
	//
	// boolean matchTags(boolean sub, String[] names) {
	// if (this.tags.length != names.length) {
	// return false;
	// }
	// for (int i = 0; i < names.length; i++) {
	// if (!this.tags[i].equals(names[i])) {
	// return false;
	// }
	// }
	// return true;
	// }

	@Override
	public <C> C mapType(TypeMapper<C> codeType) {
		return this.baseTy.mapType(codeType);
	}

	@Override
	public void strOut(StringBuilder sb) {
		OStrings.append(sb, this.baseTy);
		sb.append(" #");
		OStrings.joins(sb, this.tags, " #");
	}

	@Override
	public String keyOfArrows() {
		return this.baseTy.keyOfArrows();
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
