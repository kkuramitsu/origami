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

	public TType getType();

	public Template getTemplate(TEnv env);

	public String strOut(TEnv env);

	public void emitCode(TEnv env, TCodeSection sec);

}

interface TCodeAPI {

	public TCode self();

	public default TCode setSourcePosition(Tree<?> t) {
		return self();
	}

	public default boolean isEmpty() {
		return false;
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

	public default TCode stillUntyped() {
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

abstract interface Code0 extends TCode {
	@Override
	public default Iterator<TCode> iterator() {
		return new CodeIter0();
	}

	static class CodeIter0 implements Iterator<TCode> {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public TCode next() {
			return null;
		}
	}
}

abstract class Code1 implements TCode {
	protected TCode inner;

	Code1(TCode inner) {
		this.inner = inner;
	}

	public TCode getInner() {
		return this.inner;
	}

	@Override
	public boolean isEmpty() {
		return this.inner.isEmpty();
	}

	@Override
	public TType getType() {
		return this.inner.getType();
	}

	@Override
	public TCode setSourcePosition(Tree<?> t) {
		this.inner.setSourcePosition(t);
		return this;
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		this.inner = this.inner.asType(env, t);
		return this;
	}

	@Override
	public Iterator<TCode> iterator() {
		return new CodeIterN(this.inner);
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.inner.strOut(env));
	}

}

abstract class CodeN implements TCode {
	protected TCode[] args;

	public CodeN(TCode... args) {
		this.args = args;
	}

	public int size() {
		return this.args.length;
	}

	public TCode[] args() {
		return this.args;
	}

	@Override
	public Iterator<TCode> iterator() {
		return new CodeIterN(this.args);
	}

	@Override
	public String strOut(TEnv env) {
		switch (this.args.length) {
		case 0:
			return this.getTemplate(env).format();
		case 1:
			return this.getTemplate(env).format(this.args[0].strOut(env));
		case 2:
			return this.getTemplate(env).format(this.args[0].strOut(env), this.args[1].strOut(env));
		default:
			Object[] p = new String[this.args.length];
			for (int i = 0; i < this.args.length; i++) {
				p[i] = this.args[i].strOut(env);
			}
			return this.getTemplate(env).format(p);
		}
	}
}

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

abstract class TypedCode0 implements Code0 {

	TypedCode0(TType typed) {
		this.setType(typed);
	}

	private TType typed;

	@Override
	public TType getType() {
		return this.typed;
	}

	public void setType(TType typed) {
		assert (typed != null);
		this.typed = typed;
	}

}

abstract class TypedCode1 extends Code1 {
	private TType typed;
	protected Template template;
	protected TCode inner;

	public TypedCode1(TType ret, Template template, TCode inner) {
		super(inner);
		this.setType(ret);
		this.template = template;
		this.inner = inner;
	}

	public TypedCode1(TCode inner) {
		this(TType.tUntyped, Template.Null, inner);
	}

	@Override
	public TType getType() {
		return this.typed;
	}

	public void setType(TType typed) {
		assert (typed != null);
		this.typed = typed;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return this.template;
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.inner.strOut(env));
	}
}

abstract class TypedCodeN extends CodeN {
	private TType typed;
	protected Template template;

	TypedCodeN(TType t, Template tp, TCode... args) {
		super(args);
		this.setType(t);
		this.setTemplate(tp);
	}

	TypedCodeN(TCode... args) {
		this(TType.tUntyped, Template.Null, args);
	}

	@Override
	public TType getType() {
		return this.typed;
	}

	public void setType(TType typed) {
		assert (typed != null);
		this.typed = typed;
	}

	@Override
	public Template getTemplate(TEnv env) {
		return this.template;
	}

	public void setTemplate(Template tp) {
		this.template = tp;
	}

}
