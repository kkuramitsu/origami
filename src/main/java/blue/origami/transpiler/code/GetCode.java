package blue.origami.transpiler.code;

import blue.origami.common.SyntaxBuilder;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.CodeSection;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.type.Ty;

public class GetCode extends Code1 {
	final String name;

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
		return this.name;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		if (this.isUntyped()) {
			Ty recvTy = this.asTypeAt(env, 0, Ty.tVar(null));
			Ty fieldTy = recvTy.resolveFieldType(env, this.getSource(), this.name);
			this.setType(fieldTy);
			return this.castType(env, ret);
			//
			// if (recvTy.isVar()) {
			// Ty infer = new DataVarTy();
			// recvTy.match(TypeMatchContext.Update, bSUB, infer);
			// recvTy = infer;
			// }
			// if (recvTy.isData()) {
			// DataTy dt = (DataTy) recvTy.devar();
			// this.setType(dt.fieldType(env, this.getSource(), this.name));
			// return this.castType(env, ret);
			// }
			// throw new ErrorCode(this.getSource(), TFmt.unsupported_error);
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
