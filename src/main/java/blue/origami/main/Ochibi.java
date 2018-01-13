package blue.origami.main;

public class Ochibi extends Otranspile {

	@Override
	public boolean isREPL() {
		return true;
	}

	@Override
	public boolean isHacked() {
		return false;
	}

	@Override
	public boolean isVerbose() {
		return false;
	}

	@Override
	public boolean isDebug() {
		return false;
	}

}
