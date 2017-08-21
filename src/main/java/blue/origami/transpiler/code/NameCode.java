package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.rule.ParseRule;
import blue.origami.transpiler.type.Ty;
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
		this(nameTree.getString(), 0, null, 0);
		this.setSource(nameTree);
	}

	public NameCode(String name) {
		this(name, 0, null, 0);
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
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			NameInfo ref = env.get(this.name, NameInfo.class, (e, c) -> e.isNameInfo(env) ? e : null);
			if (ref == null) {
				throw new ErrorCode(this, TFmt.undefined_name__YY0, this.name);
			}
			return ref.nameCode().castType(env, ret);
		}
		return super.asType(env, ret);
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
