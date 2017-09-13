package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.SugarCode;
import blue.origami.transpiler.type.Ty;

public class IndexExpr implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code recv = env.parseCode(env, t.get(_recv));
		Code[] params = env.parseParams(env, t, _param);
		if (params.length != 1) {
			throw new ErrorCode(t.get(_param), TFmt.mismatched_parameter_size_S_S, params.length, 1);
		}
		return new GetIndexCode(recv, params[0]);
	}

	static class GetIndexCode extends SugarCode {
		Code recv;
		Code index;

		public GetIndexCode(Code recv, Code index) {
			this.recv = recv;
			this.index = index;
		}

		@Override
		public Code[] args() {
			return this.makeArgs(this.recv, this.index);
		}

		@Override
		public Code asType(TEnv env, Ty ret) {
			if (this.isUntyped()) {
				return new ExprCode("[]", this.recv, this.index).asType(env, ret);
			}
			return super.castType(env, ret);
		}
	}

}