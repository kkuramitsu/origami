package blue.origami.common;

public interface Handled<T> {
	public T getHandled();

	public default boolean isA(Class<?> c) {
		return c.isInstance(getHandled());
	}

	@SuppressWarnings("unchecked")
	public default <X> X getHandled(Class<X> c) {
		if (c.isInstance(getHandled())) {
			return (X) this.getHandled();
		}
		return null;
	}

}
