package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.FlowDataTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.TypeMatcher;

public class SetCode extends CodeN {
	final String name;
	private String cnt = "";

	public SetCode(Code recv, AST nameTree, Code right) {
		super(recv, right);
		// String s = nameTree.getString();
		// if (s.startsWith(".")) {
		// s = s.substring(1); // FIXME: bugs in parser
		// }
		// this.name = s;
		this.name = nameTree.getString();
		this.setSource(nameTree);
	}

	public String getName() {
		return this.name + this.cnt;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.isUntyped()) {
			Ty recvTy = this.asTypeAt(env, 0, Ty.tUntyped());
			if (recvTy.isVar()) {
				Ty infer = new FlowDataTy();
				recvTy.match(bSUB, infer, TypeMatcher.Update);
				recvTy = infer;
			}
			if (recvTy.isData()) {
				DataTy dt = (DataTy) recvTy.base();
				if (this.cnt.isEmpty()) {
					this.cnt = dt.getCnt();
				}
				this.asTypeAt(env, 1, dt.fieldTy(env, this.getSource(), this.name));
				ret.foundMutation();
				this.setType(Ty.tVoid);
				return this.castType(env, ret);
			}
			throw new ErrorCode(this.getSource(), TFmt.unsupported_error);
		}
		return this.castType(env, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushSet(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "set-" + this.name, this.args);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Expr(this.args[0]);
		sh.Token(".");
		sh.Name(this.name);
		sh.s();
		sh.Operator("=");
		sh.s();
		sh.Expr(this.args[1]);
	}

}
