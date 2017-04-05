package blue.origami.util;

public interface OptionalFactory<T> extends Cloneable {
	public Class<?> keyClass();

	public T clone();

	public void init(OOption options);
}