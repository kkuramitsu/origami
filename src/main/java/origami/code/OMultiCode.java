package origami.code;

import java.util.List;

import origami.OEnv;
import origami.asm.OAsm;
import origami.type.OType;

public class OMultiCode extends OParamCode<Void> {

	public OMultiCode(OCode... nodes) {
		super(null, nodes[nodes.length - 1].getType(), checkReturn(nodes));
	}

	public OMultiCode(List<OCode> l) {
		this(l.toArray(new OCode[l.size()]));
	}

	protected OMultiCode(int dummy, OCode... nodes) {
		super(null, nodes[nodes.length - 1].getType(), nodes);
	}

	public final boolean hasDefinedLocalVariables() {
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].isDefined()) {
				return true;
			}
		}
		return false;
	}

	public static OCode[] checkReturn(OCode[] nodes) {
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].hasReturnCode()) {
				if (i < nodes.length - 1) {
					OCode[] newnodes = new OCode[i + 1];
					System.arraycopy(nodes, 0, newnodes, 0, newnodes.length);
					return newnodes;
				}
			}
		}
		return nodes;
	}

	@Override
	public boolean hasReturnCode() {
		for (int i = 0; i < this.nodes.length; i++) {
			if (nodes[i].hasReturnCode()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public OType getType() {
		return nodes[nodes.length - 1].getType();
	}

	@Override
	public OCode refineType(OEnv env, OType t) {
		nodes[nodes.length - 1] = nodes[nodes.length - 1].refineType(env, t);
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Object evaled = null; // new OEmpty();
		for (int i = 0; i < this.nodes.length; i++) {
			evaled = nodes[i].eval(env);
		}
		return evaled;
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushMulti(this);
	}

}
