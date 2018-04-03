package blue.origami.transpiler.code;

import blue.origami.common.ODebug;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.type.Ty;
import origami.nez2.Token;

public class VarNameCode extends CommonCode {

	public final String name;
	private final int seq;
	private final int refLevel;

	public VarNameCode(Token name) {
		this(name, -1, null, 0);
	}

	public VarNameCode(String name) {
		this(NameHint.getName(name));
	}

	public VarNameCode(Token name, int seq, Ty ty, int refLevel) {
		super(ty);
		this.name = name.getSymbol();
		this.seq = seq;
		this.refLevel = refLevel;
		this.setSource(name);
	}

	public String getName() {
		return this.seq == -1 ? this.name : NameHint.safeName(this.name) + this.seq;
	}

	public int getRefLevel() {
		return this.refLevel;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		return env.getLanguage().typeName(env, this, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		try {
			sec.pushName(this);
		} catch (Exception e) {
			ODebug.trace("unfound name %s %d", this.name, this.refLevel);
			ODebug.traceException(e);
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, this.getName());
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.TypeAnnotation_(this.getType(), () -> {
			sh.Name(this.getName());
		});
	}
}
