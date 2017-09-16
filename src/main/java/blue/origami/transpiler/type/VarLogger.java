package blue.origami.transpiler.type;

import java.util.ArrayList;
import java.util.List;

import blue.origami.util.ODebug;

public class VarLogger {
	public final static VarLogger Update = new VarLogger();
	public final static VarLogger Nop = new VarLogger();

	static class VarLog {
		Ty prevTy;
		VarTy varTy;

		VarLog(VarTy v) {
			this.prevTy = v.resolvedTy;
			this.varTy = v;
		}
	}

	List<VarLog> logs = null;

	public boolean isUpdate() {
		return this != Nop;
	}

	public boolean update(VarTy v, Ty ty) {
		if (this == Nop) {
			return false;
		}
		if (this != Update) {
			if (this.logs == null) {
				this.logs = new ArrayList<>(8);
			}
			this.logs.add(new VarLog(v));
		}
		assert (v.id > 26);
		if (v.id < 26) {
			ODebug.trace("FIXME var %s do not mutate", this);
			return true;
		}
		v.resolvedTy = ty;
		return true;
	}

	public void abort() {
		if (this.logs != null) {
			for (int i = this.logs.size() - 1; i >= 0; i--) {
				VarLog log = this.logs.get(i);
				log.varTy.resolvedTy = log.prevTy;
			}
			this.logs = null;
		}
	}

}