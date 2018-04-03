package origami.main;

import blue.origami.common.OOption;
import blue.origami.common.OWriter;
import origami.nez2.PEG;
import origami.nezcc2.NezCC2;

public class Onezcc extends Oparse {

	@Override
	public void exec(OOption options) throws Throwable {
		NezCC2 pg = options.newInstance(NezCC2.class);
		PEG peg = new PEG();
		peg.load(pegFile(options, null));
		pg.emit(peg, pegStart(peg, options), new OWriter());
	}

}