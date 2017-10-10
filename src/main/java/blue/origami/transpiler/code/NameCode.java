package blue.origami.transpiler.code;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.rule.NameExpr.NameInfo;
import blue.origami.transpiler.type.Ty;
import blue.origami.util.ODebug;

public class NameCode extends CommonCode {

	private final String name;
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
	public Code bindAs(TEnv env, Ty ret) {
		return this.asType(env, ret);
	}

	@Override
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			NameInfo ref = env.get(this.name, NameInfo.class, (e, c) -> e.isNameInfo(env) ? e : null);
			if (ref != null) {
				ref.used(env);
				return ref.newNameCode(env, this.getSource()).castType(env, ret);
			}
			return this.parseNames(env, this.name, ret);
		}
		return super.asType(env, ret);
	}

	Code parseNames(TEnv env, String name, Ty ret) {
		Code mul = null;
		for (int i = 0; i < name.length(); i++) {
			String var = this.parseName(name, i);
			NameInfo ref = env.get(var, NameInfo.class, (e, c) -> e.isNameInfo(env) ? e : null);
			if (ref == null) {
				NameHint hint = env.findNameHint(env, name);
				if (hint != null) {
					return new ErrorCode(this, TFmt.undefined_name__YY1__YY2, this.name, hint.getType());
				}
				return new ErrorCode(this, TFmt.undefined_name__YY1, this.name);
			}
			ref.used(env);
			mul = this.mul(mul, ref.newNameCode(env, this.getSource()));
		}
		return mul.asType(env, ret);
	}

	private String parseName(String name, int index) {
		int end = index + 1;
		while (end < name.length()) {
			char c = name.charAt(end);
			if (Character.isDigit(c) || c == '\'') {
				end++;
			} else {
				break;
			}
		}
		return name.substring(index, end);
	}

	private Code mul(Code left, Code right) {
		if (left == null) {
			return right;
		}
		return new BinaryCode("*", left, right);
	}

	@Override
	public void emitCode(TEnv env, CodeSection sec) {
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

	@Override
	public void dumpCode(SyntaxHighlight sh) {
		sh.TypeAnnotation_(this.getType(), () -> {
			sh.Name(this.name);
		});
	}

}
