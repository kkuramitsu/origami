package blue.origami.main;

import blue.origami.common.OOption;
import blue.origami.common.OWriter;
import blue.origami.parser.nezcc.NezCC2;

public class Onezcc2 extends Main {

	@Override
	public void exec(OOption options) throws Throwable {
		NezCC2 pg = options.newInstance(NezCC2.class);
		pg.emit(this.getParser(options).getParserGrammar(), new OWriter());
	}

}