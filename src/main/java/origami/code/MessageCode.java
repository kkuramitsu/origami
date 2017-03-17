package origami.code;

import origami.OLog;
import origami.nez.ast.Tree;
import origami.trait.FormatMethods;
import origami.type.OType;

public abstract class MessageCode extends OParamCode<OLog> implements FormatMethods {
	public final int level;

	public MessageCode(int level, OLog m, OType ty, OCode... nodes) {
		super(m, ty, nodes);
		this.level = level;
		m.setSource(this.getSource());
	}

	public MessageCode(int level, OLog m, OType ty) {
		super(m, ty);
		this.level = level;
		m.setSource(this.getSource());
	}

	public OLog getLog() {
		return this.getHandled();
	}

	@Override
	public OCode setSource(Tree<?> s) {
		super.setSource(s);
		this.getLog().setSource(s);
		return this;
	}

	public String msg() {
		return this.getLog().toString();
	}

}