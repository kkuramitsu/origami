package blue.origami.transpiler.rule;

import blue.origami.konoha5.DSymbol;
import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TDataType;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TNameHint;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TErrorCode;
import blue.origami.transpiler.code.TIntCode;
import blue.origami.transpiler.code.TSugarCode;

public class GetExpr implements TTypeRule, OSymbols {
	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode recv = env.parseCode(env, t.get(_recv));
		return new TGetCode(recv, t.get(_name));
	}

	public static class TGetCode extends TSugarCode {
		TCode recv;
		final String name;
		Tree<?> nameTree;

		public TGetCode(TCode recv, Tree<?> nameTree) {
			this.recv = recv;
			this.nameTree = nameTree;
			this.name = nameTree.getString();
		}

		public TCode[] args() {
			return this.args(this.recv);
		}

		@Override
		public TCode asType(TEnv env, TType t) {
			assert (this.isUntyped());
			this.recv = this.recv.asType(env, TType.tUntyped);
			if (this.recv.isDataType()) {
				TNameHint hint = env.findNameHint(env, this.name);
				if (hint == null) {
					throw new TErrorCode(this.nameTree, TFmt.undefined_name__YY0, this.name);
				}
				TDataType dt = (TDataType) this.recv.getType();
				dt.checkGetField(this.nameTree, this.name);
				if (dt.isUntyped()) {
					return this;
				}
				TCode code = this.recv.applyMethodCode(env, "getf", new TIntCode(DSymbol.id(this.name)),
						hint.getDefaultValue());
				return code.asType(env, hint.getType()).asType(env, t);
			}
			throw new TErrorCode(this.nameTree, TFmt.unsupported_operator);
		}

	}

}
