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
import blue.origami.common.ODebug;

public class GetCode extends Code1 {
	final String name;
	private String cnt = "";

	public GetCode(Code recv, AST nameTree) {
		super(recv);
		this.name = nameTree.getString();
		this.setSource(nameTree);
	}

	public GetCode(Code recv, String name, Ty nameTy) {
		super(nameTy, recv);
		this.name = name;
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
				this.setType(dt.fieldTy(env, this.getSource(), this.name));
				return this.castType(env, ret);
			}
			throw new ErrorCode(this.getSource(), TFmt.unsupported_error);
		}
		return this.castType(env, ret);
	}

	@Override
	public void emitCode(CodeSection sec) {
		sec.pushGet(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.sexpr(sb, "get-" + this.name, this.inner);
	}

	@Override
	public void dumpCode(SyntaxBuilder sh) {
		sh.Expr(this.getInner());
		sh.Token(".");
		sh.Name(this.name);
	}

}
