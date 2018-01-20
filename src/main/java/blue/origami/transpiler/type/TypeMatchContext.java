package blue.origami.transpiler.type;

import java.util.ArrayList;
import java.util.List;

import blue.origami.common.ODebug;

public class TypeMatchContext {
	public final static TypeMatchContext Update = new TypeMatchContext();
	public final static TypeMatchContext Nop = new TypeMatchContext();

	static abstract class BackLog {
		abstract void abort();
	}

	static class VarLog extends BackLog {
		Ty prevTy;
		VarTy varTy;

		VarLog(VarTy v) {
			this.prevTy = v.varref;
			this.varTy = v;
		}

		@Override
		void abort() {
			this.varTy.varref = this.prevTy;
		}
	}

	// static class UnionLog extends BackLog {
	// Ty[] prevChoice;
	// UnionTy unionTy;
	//
	// UnionLog(UnionTy u) {
	// this.prevChoice = u.choice;
	// this.unionTy = u;
	// }
	//
	// @Override
	// void abort() {
	// this.unionTy.choice = this.prevChoice;
	// }
	// }

	List<BackLog> logs = null;

	void add(BackLog log) {
		if (this.logs == null) {
			this.logs = new ArrayList<>(8);
		}
		this.logs.add(log);
	}

	public void abort() {
		if (this.logs != null) {
			for (int i = this.logs.size() - 1; i >= 0; i--) {
				BackLog log = this.logs.get(i);
				log.abort();
			}
			this.logs = null;
		}
	}

	public boolean isUpdate() {
		return this != Nop;
	}

	public boolean updateVar(VarTy v, Ty ty) {
		if (this == Nop) {
			return false;
		}
		if (this != Update) {
			this.add(new VarLog(v));
		}
		if (v.varId < 26) {
			ODebug.trace("FIXME: var %s do not mutate", this);
			assert (v.varId > 26);
			return true;
		}
		v.varref = ty;
		return true;
	}
	//
	// public boolean updateUnion(UnionTy u, Ty... choice) {
	// if (this == Nop) {
	// return true;
	// }
	// if (this != Update) {
	// this.add(new UnionLog(u));
	// }
	// u.choice = choice;
	// return true;
	// }

}