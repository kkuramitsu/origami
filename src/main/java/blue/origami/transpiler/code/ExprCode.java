package blue.origami.transpiler.code;

import java.util.List;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.CodeMap;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.Ty;
import origami.libnez.OStrings;

public class ExprCode extends CodeN implements MappedCode {

	public String name;
	private CodeMap mapped;

	public ExprCode(String name, Code... args) {
		super(args);
		this.name = name;
		this.mapped = null;
	}

	public ExprCode(CodeMap tp, Code... args) {
		super(tp.getReturnType(), args);
		this.name = tp.getName();
		this.setMapped(tp);
	}

	@Override
	public CodeMap getMapped() {
		assert (this.mapped != null);
		return this.mapped;
	}

	public void setMapped(CodeMap tp) {
		this.mapped = tp;
	}

	@Override
	public boolean isAbstract() {
		return this.mapped.isAbstract();
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushCall(this);
	}

	@Override
	public Code asType(Env env, Ty ret) {
		return env.getLanguage().typeExpr(env, this, ret);
	}

	public int maxCost() {
		return CodeMap.BADCONV;
	}

	public Code asUnfound(Env env, List<CodeMap> l) {
		env.findList(this.name, CodeMap.class, l, (tt) -> !tt.isExpired());
		throw new ErrorCode(this, TFmt.undefined_SSS, this.name, this.msgArgs(), msgHint(env, l));
	}

	public Code asMismatched(Env env, List<CodeMap> l) {
		if (l.get(0).isMutation() && !this.args[0].getType().isMutable()) {
			throw new ErrorCode(this, TFmt.not_mutable_SSS, this.name, this.msgArgs(), msgHint(env, l));
		}
		throw new ErrorCode(this, TFmt.mismatched_SSS, this.name, this.msgArgs(), msgHint(env, l));
	}

	private String msgArgs() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		OStrings.joins(sb, this.args, ", ", p -> p.getType().memoed());
		sb.append(")");
		return sb.toString();
	}

	static String msgHint(Env env, List<CodeMap> l) {
		StringBuilder sb = new StringBuilder();
		OStrings.joins(sb, l, ", ", tp -> tp.isAbstract() ? "" : tp.getName() + ": " + tp.getFuncType());
		if (sb.length() == 0) {
			return "";
		}
		return " \t" + TFmt.hint + " " + sb;
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, this.name, this.args);
	}

	@Override
	public void dumpCode(SyntaxBuilder sb) {
		sb.TypeAnnotation(this.getType(), () -> {
			sb.Name(this.name);
			sb.Token("(");
			for (int i = 0; i < this.args.length; i++) {
				if (i > 0) {
					sb.Token(",");
				}
				sb.Expr(this.args[i]);
			}
			sb.Token(")");
		});
	}

	public static ExprCode option(String name, Code... args) {
		return new OptionalExprCode(name, args);
	}

}

class OptionalExprCode extends ExprCode implements MappedCode {

	OptionalExprCode(String name, Code... code) {
		super(name, code);
	}

	@Override
	public int maxCost() {
		return CodeMap.CAST;
	}

	@Override
	public Code asUnfound(Env env, List<CodeMap> l) {
		return this.args[0];
	}

	@Override
	public Code asMismatched(Env env, List<CodeMap> l) {
		return this.args[0];
	}

}
