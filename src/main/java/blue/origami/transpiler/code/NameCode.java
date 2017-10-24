package blue.origami.transpiler.code;

import blue.origami.common.ODebug;
import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.type.Ty;

public class NameCode extends CommonCode {

	public final String name;
	private final int seq;
	private final int refLevel;

	public NameCode(AST name) {
		this(name, -1, null, 0);
	}

	public NameCode(String name) {
		this(AST.getName(name));
	}

	public NameCode(AST name, int seq, Ty ty, int refLevel) {
		super(ty);
		this.name = name.getString();
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
	public Code bindAs(Env env, Ty ret) {
		return this.asType(env, ret);
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
