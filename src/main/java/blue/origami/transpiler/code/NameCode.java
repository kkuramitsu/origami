package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.rule.ParseRule;
import blue.origami.util.ODebug;

public class NameCode extends CommonCode implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		return new NameCode(t);
	}

	private final String name;
	private final int seq;
	private final int refLevel;

	public NameCode(Tree<?> nameTree) {
		this(nameTree.getString(), 0, Ty.tUntyped, 0);
		this.setSource(nameTree);
	}

	public NameCode(String name, int seq, Ty ty, int refLevel) {
		super(ty);
		this.name = name;
		this.seq = seq;
		this.refLevel = refLevel;
	}

	public String getName() {
		return NameHint.safeName(this.name) + this.seq;
	}

	public int getRefLevel() {
		return this.refLevel;
	}

	@Override
	public Code asType(TEnv env, Ty t) {
		if (this.isUntyped() && !this.isDataType()) {
			NameInfo ref = env.get(this.name, NameInfo.class, (e, c) -> e.isNameInfo(env) ? e : null);
			if (ref == null) {
				throw new ErrorCode(this, TFmt.undefined_name__YY0, this.name);
			}
			return ref.nameCode(env, this.name).castType(env, t);
		}
		return super.asType(env, t);
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("varname", "name", "%s");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.name);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		try {
			sec.pushName(env, this);
		} catch (Exception e) {
			ODebug.trace("unfound name %s %d", this.name, this.refLevel);
			ODebug.traceException(e);
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.name);
	}

}
