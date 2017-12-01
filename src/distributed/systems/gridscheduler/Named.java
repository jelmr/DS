package distributed.systems.gridscheduler;

/**
 * @author Jelmer Mulder
 *         Date: 01/12/2017
 */
public class Named<E> {

	private E stub;
	private String name;


	public Named(String name, E stub) {
		this.stub = stub;
		this.name = name;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Named))
			return false;

		Named<?> named = (Named<?>) o;

		return name.equals(named.name);
	}


	@Override
	public int hashCode() {
		return name.hashCode();
	}


	public E getObject() {
		return stub;
	}


	public void setStub(E stub) {
		this.stub = stub;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}
}
