package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.Template;
import blue.origami.transpiler.rule.NameExpr.TNameRef;
import blue.origami.transpiler.rule.ParseRule;
import blue.origami.util.ODebug;

public class TNameCode extends CommonCode implements ParseRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		return new TNameCode(t);
	}

	private Tree<?> nameTree = null;
	private final String lname;
	private final int refLevel;

	public TNameCode(Tree<?> nameTree) {
		this(nameTree.getString(), TType.tUntyped, 0);
		this.setSource(nameTree);
	}

	public TNameCode(String name, TType ty, int refLevel) {
		super(ty);
		this.lname = name;
		this.refLevel = refLevel;
	}

	public String getName() {
		return this.lname;
	}

	public int getRefLevel() {
		return this.refLevel;
	}

	@Override
	public TCode asType(TEnv env, TType t) {
		// ODebug.trace("finding %s %s", this.lname, t);
		if (this.isUntyped()) {
			TNameRef ref = env.get(this.lname, TNameRef.class, (e, c) -> e.isNameRef(env) ? e : null);
			if (ref == null) {
				throw new TErrorCode(this.nameTree, TFmt.undefined_name__YY0, this.lname);
			}
			return ref.nameCode(env, this.lname).asType(env, t);
		}
		return super.asType(env, t);
	}

	@Override
	public Template getTemplate(TEnv env) {
		return env.getTemplate("varname", "name", "%s");
	}

	@Override
	public String strOut(TEnv env) {
		return this.getTemplate(env).format(this.lname);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		try {
			sec.pushName(env, this);
		} catch (Exception e) {
			ODebug.trace("unfound name %s %d", this.lname, this.refLevel);
			ODebug.traceException(e);
		}
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append(this.lname);
	}

}
