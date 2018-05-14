package origami.tcode;

import origami.nez2.ParseTree;

public interface TRule {
	TCode match(TEnv env, String path, ParseTree t);
}
