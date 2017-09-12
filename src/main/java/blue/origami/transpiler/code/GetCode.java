package blue.origami.transpiler.code;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TCodeSection;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.type.DataTy;
import blue.origami.transpiler.type.FlowDataTy;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarLogger;

public class GetCode extends Code1 {
	final String name;

	public GetCode(Code recv, Tree<?> nameTree) {
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
	public Code asType(TEnv env, Ty ret) {
		if (this.isUntyped()) {
			Ty recvTy = this.asTypeAt(env, 0, Ty.tUntyped());
			if (recvTy.isVar()) {
				Ty infer = new FlowDataTy();
				recvTy.acceptTy(bSUB, infer, VarLogger.Update);
				recvTy = infer;
			}
			if (recvTy.isData()) {
				DataTy dt = (DataTy) recvTy.real();
				this.setType(dt.fieldTy(env, this.getSource(), this.name));
				return this.castType(env, ret);
			}
			throw new ErrorCode(this.getSource(), TFmt.unsupported_error);
		}
		return this.castType(env, ret);
	}

	@Override
	public void emitCode(TEnv env, TCodeSection sec) {
		sec.pushGet(env, this);
	}

	@Override
	public void strOut(StringBuilder sb) {

	}

}
