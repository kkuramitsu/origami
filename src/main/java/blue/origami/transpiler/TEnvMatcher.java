package blue.origami.transpiler;

@FunctionalInterface
public interface TEnvMatcher<X, Y> {
	public Y match(X x, Class<X> c);
}