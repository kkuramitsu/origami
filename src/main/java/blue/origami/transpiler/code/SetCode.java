package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.DataTy;
import blue.origami.transpiler.NameHint;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.VarTy;

public class SetCode extends CodeN {
	final String name;

	public SetCode(Code recv, Tree<?> nameTree, Code right) {
		super(recv, right);
		this.name = nameTree.getString();
		this.setSource(nameTree);
	}

	@Override
	public Code asType(TEnv env, Ty t) {
		if (this.isUntyped()) {
			Ty recvTy = this.asTypeAt(env, 0, Ty.tUntyped());
			if (recvTy instanceof VarTy) {
				Ty infer = Ty.tData().asParameter();
				recvTy.acceptTy(bSUB, infer, bUPDATE);
				recvTy = infer;
			}
			if (recvTy instanceof DataTy) {
				NameHint hint = env.findGlobalNameHint(env, this.name);
				if (hint == null) {
					throw new ErrorCode(this.getSource(), TFmt.undefined_name__YY0, this.name);
				}
				DataTy dt = (DataTy) recvTy;
				dt.checkSetField(this.getSource(), this.name);
				this.asTypeAt(env, 1, Ty.selfTy(hint.getType(), dt));
				this.setType(Ty.tVoid);
				return this.castType(env, t);
			}
			throw new ErrorCode(this.getSource(), TFmt.unsupported_operator);
		}
		return this.castType(env, t);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushSet(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {

	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
