package origami.code;

import origami.type.OType;

public abstract class OMatchedCode<T> extends OParamCode<T> {
	private int matchCost;

	public OMatchedCode(T handled, OType ty, OCode[] nodes, int cost) {
		super(handled, ty, nodes);
		this.matchCost = cost;
	}

	@Override
	public int getMatchCost() {
		return matchCost;
	}

	protected void setMatchCost(int cost) {
		matchCost = cost;
	}

}