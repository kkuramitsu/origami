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
import blue.origami.transpiler.rule.GetExpr.TGetCode;
import blue.origami.transpiler.rule.IndexExpr.TGetIndexCode;

public class AssignExpr implements ParseRule, Symbols {
	@Override
	public Code apply(TEnv env, Tree<?> t) {
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		if (left instanceof TGetCode) {
			return new TSetCode(((TGetCode) left).recv, ((TGetCode) left).nameTree, right);
		}
		if (left instanceof TGetIndexCode) {
			return new TSetIndexCode(((TGetIndexCode) left).recv, ((TGetIndexCode) left).at,
					((TGetIndexCode) left).index, right);
		}
		throw new ErrorCode(t.get(_right), TFmt.no_more_assignment);
	}

	public static class TSetCode extends SugarCode {
		Code recv;
		String name;
		Tree<?> nameTree;
		Code right;

		public TSetCode(Code recv, Tree<?> nameTree, Code right) {
			this.recv = recv;
			this.nameTree = nameTree;
			this.name = nameTree.getString();
			this.right = right;
		}

		public Code[] args() {
			return this.makeArgs(this.recv, this.right);
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
				dt.checkSetField(this.nameTree, this.name);
				if (dt.isUntyped()) {
					return this;
				}
				Code code = this.recv.applyMethodCode(env, "setf", new IntCode(DSymbol.id(this.name)), this.right);
				return code.asType(env, t);
			}
			throw new ErrorCode(this.nameTree, TFmt.unsupported_operator);
		}

	}

	public static class TSetIndexCode extends SugarCode {
		Code recv;
		Tree<?> at;
		Code index;
		Code right;

		public TSetIndexCode(Code recv, Tree<?> at, Code index, Code right) {
			this.recv = recv;
			this.at = at;
			this.index = index;
			this.right = right;
		}

		public Code[] args() {
			return this.makeArgs(this.recv, this.index, this.right);
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
				this.right = this.right.asType(env, Ty.tUntyped);
				dt.checkSetIndex(this.at, this.right.getType());
				if (dt.isUntyped()) {
					return this;
				}
			}
			Code code = this.recv.applyMethodCode(env, "[]=", this.index, this.right);
			return code.asType(env, t);
		}

	}

}