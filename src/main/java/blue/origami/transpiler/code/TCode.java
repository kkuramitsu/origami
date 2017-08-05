package blue.origami.transpiler.code;

import java.util.Iterator;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TDataType;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.TCastCode.TConvTemplate;

public interface TCode extends TCodeAPI, Iterable<TCode> {
	@Override
	public default TCode self() {
		return this;
	}

	public TCode setSource(Tree<?> t);

	public Tree<?> getSource();

	public TCode[] args();

	public TType getType();

	public Template getTemplate(TEnv env);

	public String strOut(TEnv env);

	public void emitCode(TEnv env, TCodeSection sec);

	@Override
	public default Iterator<TCode> iterator() {
		return TCodeAPI.super.iterator();
	}

}

interface TCodeAPI {

	public TCode self();

	public default boolean isEmpty() {
		return false;
	}

	public default int size() {
		return self().args().length;
	}

	public default Iterator<TCode> iterator() {
		class CodeIterN implements Iterator<TCode> {
			int loc;
			protected TCode[] args;

			CodeIterN(TCode... args) {
				this.args = args;
				this.loc = 0;
			}

			@Override
			public boolean hasNext() {
				return this.loc < this.args.length;
			}

			@Override
			public TCode next() {
				return this.args[this.loc++];
			}
		}
		return new CodeIterN(this.self().args());
	}

	public default boolean isUntyped() {
		return self().getType().isUntyped();
	}

	public default boolean isDataType() {
		return self().getType() instanceof TDataType;
	}

	public default int countUntyped(int count) {
		if (this.isUntyped()) {
			count++;
		}
		for (TCode c : self()) {
			count = c.countUntyped(count);
		}
		return count;
	}

	public default TCode asType(TEnv env, TType t) {
		return asExactType(env, t);
	}

	public default TCode asExactType(TEnv env, TType t) {
		TCode self = self();
		TType f = self.getType();
		if (self.isUntyped() || t.accept(self)) {
			return self;
		}
		TConvTemplate tt = env.findTypeMap(env, f, t);
		if (tt == TConvTemplate.Stupid) {

		}
		return new TCastCode(t, tt, self);
	}

	public default TCode StillUntyped() {
		return self();
	}

	public default TType guessType() {
		return self().getType();
	}

	public default TCode goingOut() {
		TType t = self().getType();
		if (t instanceof TDataType) {
			// t.asLocal();
		}
		return self();
	}

	public default boolean hasReturn() {
		return false;
	}

	public default TCode addReturn() {
		return new TReturnCode(self());
	}

	public default TCode applyCode(TEnv env, TCode... params) {
		return new TApplyCode(TArrays.join(self(), params));
	}

	public default TCode applyMethodCode(TEnv env, String name, TCode... params) {
		return new TExprCode(name, TArrays.join(self(), params));
	}
}

abstract class CommonCode implements TCode {
	Tree<?> at;
	TType typed;
	Template template;

	protected CommonCode(TType t) {
		this.at = null;
		this.typed = t;
		this.template = null;
	}

	protected CommonCode() {
		this(TType.tUntyped);
	}

	@Override
	public Tree<?> getSource() {
		if (this.at == null) {
			for (TCode c : this.args()) {
				Tree<?> at = c.getSource();
				if (at != null) {
					return at;
				}
			}
		}
		return this.at;
	}

	@Override
	public TCode setSource(Tree<?> t) {
		this.at = t;
		return this;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public TCode[] args() {
		return TArrays.emptyCodes;
	}

	// @Override
	// public TCode asType(TEnv env, TType t) {
	// if (this.typed == null) {
	// for (TCode c : this.args()) {
	// c.asType(env, t);
	// }
	// }
	// return super.asType(env, t);
	// }

	public void setType(TType typed) {
		assert (typed != null);
		this.typed = typed;
	}

	@Override
	public TType getType() {
		if (this.typed == null) {
			TCode[] a = this.args();
			if (a.length == 0) {
				return TType.tVoid;
			}
			return a[a.length - 1].getType();
		}
		return this.typed;
	}

	public void setTemplate(Template tp) {
		this.template = tp;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return this.template;
	}

	@Override
	public String strOut(TEnv env) {
		TCode[] a = this.args();
		switch (a.length) {
		case 0:
			return this.getTemplate(env).format();
		case 1:
			return this.getTemplate(env).format(a[0].strOut(env));
		case 2:
			return this.getTemplate(env).format(a[0].strOut(env), a[1].strOut(env));
		default:
			Object[] p = new Object[a.length];
			for (int i = 0; i < a.length; i++) {
				p[i] = a[i].strOut(env);
			}
			return this.getTemplate(env).format(p);
		}
	}

}

abstract class Code1 extends CommonCode {
	protected TCode inner;

	Code1(TType t, TCode inner) {
		super(t);
		this.inner = inner;
	}

	Code1(TCode inner) {
		this(TType.tUntyped, inner);
	}

	public TCode getInner() {
		return this.inner;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public TCode[] args() {
		return new TCode[] { this.inner };
	}

}

abstract class CodeN extends CommonCode {
	protected TCode[] args;

	public CodeN(TType t, TCode... args) {
		super(t);
		this.args = args;
	}

	public CodeN(TCode... args) {
		this(TType.tUntyped, args);
	}

	@Override
	public int size() {
		return this.args.length;
	}

	@Override
	public TCode[] args() {
		return this.args;
	}

}
