package core;

import java.util.function.Predicate;

public interface Device {

	public abstract String getName();

	public abstract String getAlias();

	public abstract String getSimpleName();

	public abstract String getFullName();

	public abstract Device findDevice(Predicate<Device> criteria);

	public abstract Device findDeviceRel(String relName);

}
