package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.SugarCode;
import blue.origami.transpiler.type.Ty;

public class SliceExpr implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, AST t) {
		Code recv = env.parseCode(env, t.get(_recv));
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		return new GetSliceCode(recv, left, right);
	}

	static class GetSliceCode extends SugarCode {
		Code recv;
		Code left;
		Code right;

		public GetSliceCode(Code recv, Code left, Code right) {
			this.recv = recv;
			this.left = left;
			this.right = right;
		}

		@Override
		public Code[] args() {
			return this.makeArgs(this.recv, this.left, this.right);
		}

		@Override
		public Code asType(Env env, Ty ret) {
			if (this.isUntyped()) {
				return new ExprCode("[]", this.recv, this.left, this.right).asType(env, ret);
			}
			return super.castType(env, ret);
		}
	}
}
