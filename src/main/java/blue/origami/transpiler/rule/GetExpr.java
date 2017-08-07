package blue.origami.transpiler.rule;

import blue.origami.konoha5.DSymbol;
import blue.origami.nez.ast.Tree;

import blue.origami.transpiler.DataTy;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TNameHint;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.IntCode;
import blue.origami.transpiler.code.SugarCode;

public class GetExpr implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code recv = env.parseCode(env, t.get(_recv));
		return new TGetCode(recv, t.get(_name));
	}

	public static class TGetCode extends SugarCode {
		Code recv;
		final String name;
		Tree<?> nameTree;

		public TGetCode(Code recv, Tree<?> nameTree) {
			this.recv = recv;
			this.nameTree = nameTree;
			this.name = nameTree.getString();
		}

		public Code[] args() {
			return this.makeArgs(this.recv);
		}

		@Override
		public Code asType(TEnv env, Ty t) {
			assert (this.isUntyped());
			this.recv = this.recv.asType(env, Ty.tUntyped);
			if (this.recv.isDataType()) {
				TNameHint hint = env.findNameHint(env, this.name);
				if (hint == null) {
					throw new ErrorCode(this.nameTree, TFmt.undefined_name__YY0, this.name);
				}
				DataTy dt = (DataTy) this.recv.getType();
				dt.checkGetField(this.nameTree, this.name);
				if (dt.isUntyped()) {
					return this;
				}
				Code code = this.recv.applyMethodCode(env, "getf", new IntCode(DSymbol.id(this.name)),
						hint.getDefaultValue());
				return code.asType(env, hint.getType()).asType(env, t);
			}
			throw new ErrorCode(this.nameTree, TFmt.unsupported_operator);
		}

	}

}
