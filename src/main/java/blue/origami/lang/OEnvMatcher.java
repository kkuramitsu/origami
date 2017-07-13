package blue.origami.lang;

@FunctionalInterface
public interface OEnvMatcher<X, Y> {
	public Y match(X x, Class<X> c);
}