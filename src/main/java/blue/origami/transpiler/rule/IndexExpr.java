package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TDataType;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TSugarCode;

public class IndexExpr implements ParseRule, OSymbols {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode recv = env.parseCode(env, t.get(_recv));
		TCode[] params = env.parseParams(env, t, _param);
		if (params.length != 1) {
			throw new TErrorCode(t.get(_param), TFmt.syntax_error);
		}
		return new TGetIndexCode(recv, t.get(_param), params[0]);
	}

	public static class TGetIndexCode extends TSugarCode {
		TCode recv;
		Tree<?> at;
		TCode index;

		public TGetIndexCode(TCode recv, Tree<?> at, TCode index) {
			this.recv = recv;
			this.at = at;
			this.index = index;
		}

		public TCode[] args() {
			return this.args(this.recv, this.index);
		}

		@Override
		public TCode asType(TEnv env, TType t) {
			assert (this.isUntyped());
			this.recv = this.recv.asType(env, TType.tUntyped);
			this.index = this.index.asType(env, TType.tUntyped);
			if (this.index.isUntyped()) {
				return this;
			}
			if (this.recv.isDataType()) {
				TDataType dt = (TDataType) this.recv.getType();
				dt.checkGetIndex(this.at, TType.tUntyped);
				if (dt.isUntyped()) {
					return this;
				}
				TCode code = this.recv.applyMethodCode(env, "[]", this.index);
				return code.asType(env, dt.getInnerType()).asType(env, t);
			} else {
				return this.recv.applyMethodCode(env, "[]", this.index).asType(env, t);
			}
		}

	}

}