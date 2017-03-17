package origami.code;

import java.util.Optional;

import origami.OEnv;
import origami.asm.OAsm;

/**
 * <pre>
 * Params 0 : Init Clause (StmtCode?) 1 : Condition (OCode?) 2 : Iter Clause
 * (OCode?) 3 : Body Clause (MultiCode)
 **/

public class ForCode extends StmtCode {

	public ForCode(OEnv env, OCode... nodes) {
		super(env, "for", nodes);
	}

	@Override
	public Object eval(OEnv env) {
		initClause().ifPresent((init) -> {
			try {
				Object iter = init.eval(env);
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		});
		condition().ifPresent((cond) -> {
			try {
				while (((boolean) cond.eval(env))) {
					bodyClause().eval(env);
					iterClause().ifPresent((iter) -> {
						try {
							iter.eval(env);
						} catch (Throwable throwable) {
							throwable.printStackTrace();
						}
					});
				}
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		});
		return null;
	}

	@Override
	public void generate(OAsm gen) {
		gen.pushLoop(this);
	}

	public Optional<OCode> initClause() {
		return Optional.ofNullable(nodes[0]);
	}

	public Optional<OCode> condition() {
		return Optional.ofNullable(nodes[1]);
	}

	public Optional<OCode> iterClause() {
		return Optional.ofNullable(nodes[2]);
	}

	public OCode bodyClause() {
		return nodes[3];
	}
}
