package origami.xdevel;

import java.util.ArrayList;

import origami.code.OCode;
import origami.code.OCodeWriter;
import origami.code.OErrorCode;
import origami.code.OGenerator;
import origami.code.OSugarCode;
import origami.code.OWarningCode;
import origami.lang.OEnv;
import origami.util.OLog;

public abstract class SourceCodeWriter extends OCodeWriter implements OGenerator {

	@Override
	public void write(OEnv env, OCode code) throws Throwable {
		this.push(code);
	}

	@Override
	public void writeln(OEnv env, OCode node) throws Throwable {
		this.logList = new ArrayList<>();
		this.write(env, node);
		this.println();
		for (OLog log : this.logList) {
			OLog.report(env, log);
		}
		this.logList = null;
	}

	private ArrayList<OLog> logList = null;

	@Override
	public void pushError(OErrorCode node) {
		this.logList.add(node.getLog());

	}

	@Override
	public void pushWarning(OWarningCode node) {
		this.logList.add(node.getLog());
		this.push(node.getFirst());
	}

	@Override
	public void pushSugar(OSugarCode code) {
		code.desugar().generate(this);
	}

}
