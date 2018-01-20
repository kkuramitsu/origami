package blue.origami.transpiler.rule;

import java.util.ArrayList;
import java.util.Arrays;

import blue.origami.common.ODebug;
import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.FuncEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;
import blue.origami.transpiler.code.ErrorCode;
import blue.origami.transpiler.code.ExprCode;
import blue.origami.transpiler.code.GetCode;
import blue.origami.transpiler.code.LetCode;
import blue.origami.transpiler.code.MultiCode;
import blue.origami.transpiler.code.SetCode;
import blue.origami.transpiler.code.SugarCode;
import blue.origami.transpiler.code.TupleCode;
import blue.origami.transpiler.code.TupleIndexCode;
import blue.origami.transpiler.code.VarNameCode;
import blue.origami.transpiler.rule.IndexExpr.GetIndexCode;
import blue.origami.transpiler.type.TupleTy;
import blue.origami.transpiler.type.Ty;

public class AssignExpr implements ParseRule, Symbols {
	@Override
	public Code apply(Env env, AST t) {
		Code left = env.parseCode(env, t.get(_left));
		Code right = env.parseCode(env, t.get(_right));
		if (left instanceof GetCode) {
			return new SetCode(((GetCode) left).args()[0], ((GetCode) left).getSource(), right);
		}
		if (left instanceof GetIndexCode) {
			return new ExprCode("[]=", ((GetIndexCode) left).recv, ((GetIndexCode) left).index, right);
		}
		if (left instanceof TupleCode) {
			return new TupleAssignCode((TupleCode) left, right);
		}
		ODebug.log(() -> {
			ODebug.p("No Assignment %s %s", left.getClass().getSimpleName(), left);
		});
		throw new ErrorCode(t.get(_right), TFmt.no_more_assignment);
	}
}

class TupleAssignCode extends SugarCode {

	private TupleCode left;
	private Code right;

	public TupleAssignCode(TupleCode left, Code right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public Code asType(Env env, Ty ret) {
		Ty[] ts = new Ty[this.left.size()];
		for (int i = 0; i < ts.length; i++) {
			ts[i] = Ty.tVar(null);
		}
		TupleTy tupleTy = (TupleTy) Ty.tTuple(ts);
		this.right = this.right.asType(env, tupleTy);
		String[] names = Arrays.stream(this.left.args()).map(n -> {
			if (n instanceof VarNameCode) {
				return ((VarNameCode) n).getName();
			} else {
				throw new ErrorCode(n, TFmt.not_name);
			}
		}).toArray(String[]::new);
		ArrayList<LetCode> l = new ArrayList<>();
		for (int i = 0; i < names.length; i++) {
			if (!names[i].equals("_")) {
				l.add(new LetCode(names[i], new TupleIndexCode(this.right, i)));
			}
		}
		ODebug.trace("::: %s", l);
		FuncEnv fenv = env.getFuncEnv();
		if (fenv.isGlobalScope()) {
			l.forEach(c -> c.defineAsGlobal(env, false));
			return new DoneCode();
		}
		if (l.size() == 1) {
			return l.get(0).asType(env, Ty.tVoid);
		}
		return new MultiCode(l.stream().map(c -> c.asType(env, Ty.tVoid)).toArray(Code[]::new));
	}
}