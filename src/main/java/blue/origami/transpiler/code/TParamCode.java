package blue.origami.transpiler.code;

import blue.origami.transpiler.TTemplate;

public class TParamCode extends TArgCode {
	private int matchCost;

	public TParamCode(TTemplate template, TCode... args) {
		super(template.getReturnType(), template, args);
		this.matchCost = this.check();
	}

	private int check() {
		return 0;
	}

	public int getMatchCost() {
		return this.matchCost;
	}

}