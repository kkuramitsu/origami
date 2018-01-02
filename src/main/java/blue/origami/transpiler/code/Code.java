package blue.origami.transpiler.code;

import java.util.Iterator;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import blue.origami.common.OArrays;
import blue.origami.common.OConsole;
import blue.origami.common.ODebug;
import blue.origami.common.OStrings;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.code.DataEmptyCode;

public interface Code extends CodeAPI, Iterable<Code>, OStrings {
	@Override
	public default Code self() {
		return this;
	}

	public Code setSource(AST t);

	public AST getSource();

	public Code[] args();

	@Override
	public default Iterator<Code> iterator() {
		return CodeAPI.super.iterator();
	}

	public Ty getType();

	public void emitCode(CodeSection sec);

	// public void dumpCode(SyntaxHighlight sh);

	public default void dumpCode(SyntaxBuilder sh) {
		sh.append(this);
	}

	public default void dump() {
		SyntaxBuilder sh = new SyntaxBuilder();
		dumpCode(sh);
		OConsole.println(sh.toString());
	}

	public final static boolean bSUB = true;
	public final static boolean bEQ = false;
	public final static boolean bUPDATE = true;
	public final static boolean bTEST = false;

}

interface CodeAPI {

	public Code self();

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

	public default boolean isAbstract() {
		return false;
	}

	public default boolean isError() {
		return self() instanceof ErrorCode;
	}

	public default boolean hasSome(Predicate<Code> f) {
		if (f.test(self())) {
			return true;
		}
		for (Code c : self()) {
			if (c.hasSome(f)) {
				return true;
			}
		}
		return false;
	}

	public default boolean isDataType() {
		return self().getType() instanceof DataTy;
	}

	public default Code asType(Env env, Ty ret) {
		return castType(env, ret);
	}

	public default Code castType(Env env, Ty ret) {
		Code self = self();
		// ODebug.trace("casting %s => %s", self.getType(), ret0);
		if (ret.match(self)) {
			return self;
		}else if (self instanceof DataEmptyCode) {
			return self;
		}else if (self instanceof DataCode) {
			return ((DataCode)self).cast(env, ret);
		}
		Ty f = self.getType();
		CodeMap tp = env.findArrow(env, f, ret);
		// ODebug.trace("found map %s", tp);
		if (tp == CodeMap.StupidArrow) {
			ODebug.log(() -> {
				ODebug.stackTrace("TYPE ERROR %s => %s", f, ret);
			});
			throw new ErrorCode(self, TFmt.type_error_YY1_YY2, f.memoed(), ret.memoed());
		}
		return new CastCode(ret, tp, self);
	}

	public default Code bindAs(Env env, Ty ret) {
		return ExprCode.option("=", self()).asType(env, ret);
	}

	public default Ty guessType() {
		return self().getType();
	}

	public default Code add(Code c) {
		return new MultiCode(self(), c);
	}

	public default boolean hasReturn() {
		return false;
	}

	public default Code addReturn() {
		return new ReturnCode(self());
	}

	public default Code applyCode(Env env, Code... params) {
		return new ApplyCode(OArrays.join(self(), params));
	}

	public default Code applyMethodCode(Env env, String name, Code... params) {
		return new ExprCode(name, OArrays.join(self(), params));
	}

	public default boolean isGenerative() {
		return true;
	}

	public default boolean showError(Env env) {
		boolean b = false;
		for (Code a : self().args()) {
			if (a.showError(env)) {
				b = true;
			}
		}
		return b;
	}

	public default void sexpr(StringBuilder sb, String op, int s, int e, IntConsumer f) {
		sb.append("(");
		sb.append(op);
		for (int i = s; i < e; i++) {
			sb.append(" ");
			f.accept(i);
		}
		Ty t = self().getType();
		if (t != null) {
			sb.append(" :");
			t.strOut(sb);
		}
		sb.append(")");
	}

	public default void sexpr(StringBuilder sb, String op, Code... args) {
		sexpr(sb, op, 0, args.length, (n) -> args[n].strOut(sb));
	}

}

abstract class CommonCode implements Code {
	private AST at;
	private Ty typed;

	protected CommonCode(Ty t) {
		this.at = null;
		this.typed = t;
	}

	protected CommonCode() {
		this(null);
	}

	@Override
	public AST getSource() {
		if (this.at == null) {
			for (Code c : this.args()) {
				AST at = c.getSource();
				if (at != null) {
					return at;
				}
			}
		}
		return this.at;
	}

	@Override
	public Code setSource(AST s) {
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
		return OArrays.emptyCodes;
	}

	public Ty getTypeAt(Env env, int index) {
		assert (index < 0);
		return null;
	}

	public Ty asTypeAt(Env env, int index, Ty ret) {
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
		return this.typed;
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		String op = this.getClass().getSimpleName().replace("Code", "").toLowerCase();
		this.sexpr(sb, op, this.args());
	}

}

abstract class Code1 extends CommonCode {
	public Code inner;

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
	public Ty getTypeAt(Env env, int index) {
		assert (index == 0);
		return this.inner.getType();
	}

	@Override
	public Ty asTypeAt(Env env, int index, Ty ret) {
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
	public Code[] args;

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
	public Ty getTypeAt(Env env, int index) {
		return this.args[index].getType();
	}

	@Override
	public Ty asTypeAt(Env env, int index, Ty ret) {
		this.args[index] = this.args[index].asType(env, ret);
		return this.args[index].getType();
	}

}
