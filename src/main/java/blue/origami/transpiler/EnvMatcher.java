package blue.origami.transpiler;

@FunctionalInterface
public interface EnvMatcher<X, Y> {
	public Y match(X x, Class<X> c);
}