package blue.origami.transpiler.code;

import java.util.Iterator;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.TCastCode.TConvTemplate;

public interface TCode extends TCodeAPI, Iterable<TCode> {
	public TType getType();

	public Template getTemplate(TEnv env);

	public String strOut(TEnv env);

	public void emitCode(TEnv env, TCodeSection sec);

	public default TCode setSourcePosition(Tree<?> t) {
		return this;
	}

	@Override
	public default TCode self() {
		return this;
	}

}

interface TCodeAPI {
	TCode self();

	public default TCode asType(TEnv env, TType t) {
		TCode self = self();
		TType f = self.getType();
		if (t.equals(f)) {
			return self;
		}
		TConvTemplate tt = env.findTypeMap(env, f, t);
		return new TCastCode(t, tt, self);
	}

	public default TCode applyCode(TEnv env, TCode... params) {
		return self();
	}

	public default TCode op(TEnv env, String op, TCode right) {
		return self();
	}
}

abstract interface EmptyCode extends TCode {
	@Override
	public default Iterator<TCode> iterator() {
		return new EmptyCodeIterator();
	}

	static class EmptyCodeIterator implements Iterator<TCode> {
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

abstract class SingleCode implements TCode {
	protected TCode inner;

	SingleCode(TCode inner) {
		this.inner = inner;
	}

	public TCode getInner() {
		return this.inner;
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
		return new MultiCodeIterator(this.inner);
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.inner.strOut(env));
	}

}

abstract class MultiCode implements TCode {
	protected TCode[] args;

	public MultiCode(TCode... args) {
		this.args = args;
	}

	@Override
	public Iterator<TCode> iterator() {
		return new MultiCodeIterator(this.args);
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

class MultiCodeIterator implements Iterator<TCode> {
	int loc;
	protected TCode[] args;

	MultiCodeIterator(TCode... args) {
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

abstract class EmptyTypedCode implements EmptyCode {
	private TType typed;

	EmptyTypedCode(TType typed) {
		this.setType(typed);
	}

	@Override
	public TType getType() {
		return this.typed;
	}

	public void setType(TType typed) {
		assert (typed != null);
		this.typed = typed;
	}

}

abstract class SingleTypedCode extends SingleCode {
	private TType typed;
	protected Template template;
	protected TCode inner;

	public SingleTypedCode(TType ret, Template template, TCode inner) {
		super(inner);
		this.setType(ret);
		this.template = template;
		this.inner = inner;
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

abstract class MultiTypedCode extends MultiCode {
	private TType typed;
	protected Template template;

	MultiTypedCode(TType t, Template template, TCode... args) {
		super(args);
		this.setType(t);
		this.template = template;
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

}
