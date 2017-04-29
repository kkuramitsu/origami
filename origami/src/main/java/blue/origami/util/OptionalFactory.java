package blue.origami.util;

public interface OptionalFactory<T> extends Cloneable {
	public Class<?> keyClass();

	public T clone();

	public void init(OOption options);

	@SuppressWarnings("unchecked")
	public default T newClone() {
		try {
			return (T) this.getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			OConsole.exit(1, e);
			return null;
		}
	}
}