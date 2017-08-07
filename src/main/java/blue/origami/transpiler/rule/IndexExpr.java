package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.DataTy;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.SugarCode;

public class IndexExpr implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code recv = env.parseCode(env, t.get(_recv));
		Code[] params = env.parseParams(env, t, _param);
		if (params.length != 1) {
			throw new ErrorCode(t.get(_param), TFmt.syntax_error);
		}
		return new TGetIndexCode(recv, t.get(_param), params[0]);
	}

	public static class TGetIndexCode extends SugarCode {
		Code recv;
		Tree<?> at;
		Code index;

		public TGetIndexCode(Code recv, Tree<?> at, Code index) {
			this.recv = recv;
			this.at = at;
			this.index = index;
		}

		public Code[] args() {
			return this.makeArgs(this.recv, this.index);
		}

		@Override
		public Code asType(TEnv env, Ty t) {
			assert (this.isUntyped());
			this.recv = this.recv.asType(env, Ty.tUntyped);
			this.index = this.index.asType(env, Ty.tUntyped);
			if (this.index.isUntyped()) {
				return this;
			}
			if (this.recv.isDataType()) {
				DataTy dt = (DataTy) this.recv.getType();
				dt.checkGetIndex(this.at, Ty.tUntyped);
				if (dt.isUntyped()) {
					return this;
				}
				Code code = this.recv.applyMethodCode(env, "[]", this.index);
				return code.asType(env, dt.getInnerType()).asType(env, t);
			} else {
				return this.recv.applyMethodCode(env, "[]", this.index).asType(env, t);
			}
		}

	}

}