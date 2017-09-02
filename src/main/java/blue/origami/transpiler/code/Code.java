package blue.origami.transpiler.code;

import java.util.Iterator;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TArrays;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.code.CastCode.TConvTemplate;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;
import blue.origami.util.StringCombinator;

public interface Code extends CodeAPI, Iterable<Code>, StringCombinator {
	@Override
	public default Code self() {
		return this;
	}

	public Code setSource(Tree<?> t);

	public Tree<?> getSource();

	public Code[] args();

	@Override
	public default Iterator<Code> iterator() {
		return CodeAPI.super.iterator();
	}

	public Ty getType();

	public void emitCode(TEnv env, TCodeSection sec);

	public final static boolean bSUB = true;
	public final static boolean bEQ = false;
	public final static boolean bUPDATE = true;
	public final static boolean bTEST = false;

}

interface CodeAPI {

	public Code self();

	public default boolean isEmpty() {
		return false;
	}

	public default int size() {
		return self().args().length;
	}

	public default Iterator<Code> iterator() {
		class CodeIterN implements Iterator<Code> {
			int loc;
			protected Code[] args;

			CodeIterN(Code... args) {
				this.args = args;
				this.loc = 0;
			}

			@Override
			public boolean hasNext() {
				return this.loc < this.args.length;
			}

			@Override
			public Code next() {
				return this.args[this.loc++];
			}
		}
		return new CodeIterN(this.self().args());
	}

	public default boolean isUntyped() {
		return Ty.isUntyped(self().getType());
	}

	public default boolean isDataType() {
		return self().getType() instanceof DataTy;
	}

	public default boolean hasErrorCode() {
		if (this instanceof ErrorCode) {
			return true;
		}
		for (Code c : self()) {
			if (c.hasErrorCode()) {
				return true;
			}
		}
		return false;
	}

	public default int countUntyped(int count) {
		if (this.isUntyped()) {
			count++;
		}
		for (Code c : self()) {
			count = c.countUntyped(count);
		}
		return count;
	}

	public default Code asType(TEnv env, Ty ret) {
		return castType(env, ret);
	}

	public default Code castType(TEnv env, Ty ret) {
		Code self = self();
		Ty f = self.getType();
		if (ret.accept(self)) {
			// ODebug.trace("unnecessary cast %s => %s", f, ret);
			return self;
		}
		Template tp = env.findTypeMap(env, f, ret);
		if (tp == TConvTemplate.Stupid) {
			new Exception().printStackTrace();
			return new ErrorCode(self, TFmt.type_error_YY0_YY1, f, ret);
		}
		return new CastCode(ret, tp, self);
	}

	public default Code bind(Ty ret) {
		return ExprCode.option(ret.isMutable() ? "bindM" : "bind", self());
	}

	public default Ty guessType() {
		return self().getType();
	}

	public default Code goingOut() {
		Ty t = self().getType();
		if (t instanceof DataTy) {
			// t.asLocal();
		}
		return self();
	}

	public default boolean hasReturn() {
		return false;
	}

	public default Code addReturn() {
		return new ReturnCode(self());
	}

	public default Code applyCode(TEnv env, Code... params) {
		return new ApplyCode(TArrays.join(self(), params));
	}

	public default Code applyMethodCode(TEnv env, String name, Code... params) {
		return new ExprCode(name, TArrays.join(self(), params));
	}

}

abstract class CommonCode implements Code {
	Tree<?> at;
	Ty typed;

	protected CommonCode(Ty t) {
		this.at = null;
		this.typed = t;
	}

	protected CommonCode() {
		this(null);
	}

	@Override
	public Tree<?> getSource() {
		if (this.at == null) {
			for (Code c : this.args()) {
				Tree<?> at = c.getSource();
				if (at != null) {
					return at;
				}
			}
		}
		return this.at;
	}

	@Override
	public Code setSource(Tree<?> s) {
		if (this.at == null) {
			this.at = s;
		}
		return this;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public Code[] args() {
		return TArrays.emptyCodes;
	}

	public Ty getTypeAt(TEnv env, int index) {
		assert (index < 0);
		return null;
	}

	public Ty asTypeAt(TEnv env, int index, Ty ret) {
		assert (index < 0);
		return null;
	}

	public void setType(Ty typed) {
		assert (typed != null);
		this.typed = typed;
	}

	protected static final Ty AutoType = Ty.tAuto;

	@Override
	public Ty getType() {
		if (this.typed == AutoType) {
			Code[] a = this.args();
			if (a.length == 0) {
				return Ty.tVoid;
			}
			return a[a.length - 1].getType();
		}
		if (this.typed != null) {
			Ty ty = this.typed.staticTy();
			if (ty != this.typed) {
				ODebug.trace("nomTy %s --> %s", this.typed, ty);
				this.typed = ty;
			}
		}
		return this.typed;
	}

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}

}

abstract class Code1 extends CommonCode {
	protected Code inner;

	Code1(Ty t, Code inner) {
		super(t);
		this.inner = inner;
	}

	Code1(Code inner) {
		this(null, inner);
	}

	public Code getInner() {
		return this.inner;
	}

	@Override
	public Ty getTypeAt(TEnv env, int index) {
		assert (index == 0);
		return this.inner.getType();
	}

	@Override
	public Ty asTypeAt(TEnv env, int index, Ty ret) {
		assert (index == 0);
		this.inner = this.inner.asType(env, ret);
		return this.inner.getType();
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public Code[] args() {
		return new Code[] { this.inner };
	}

}

abstract class CodeN extends CommonCode {
	protected Code[] args;

	public CodeN(Ty t, Code... args) {
		super(t);
		this.args = args;
	}

	public CodeN(Code... args) {
		this(null, args);
	}

	@Override
	public int size() {
		return this.args.length;
	}

	@Override
	public Code[] args() {
		return this.args;
	}

	@Override
	public Ty getTypeAt(TEnv env, int index) {
		return this.args[index].getType();
	}

	@Override
	public Ty asTypeAt(TEnv env, int index, Ty ret) {
		this.args[index] = this.args[index].asType(env, ret);
		return this.args[index].getType();
	}

}
