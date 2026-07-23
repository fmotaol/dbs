package core.performer;

import java.util.function.Predicate;

import core.Device;

public class EmptyPerformer implements SimplePerformer, Device {

	@Override
	public Result perform(String templateSQL, Context context) {
		return null; // nada
	}

	@Override
	public void showTree() {
		System.out.println("(sem ação)");
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public String getSimpleName() {
		return "";
	}

	@Override
	public String getFullName() {
		return "";
	}

	@Override
	public Device findDeviceRel(String relName) {
		return null;
	}

	public Device findDevice(Predicate<Device> criteria) {
		return null;
	}

	@Override
	public String getAlias() {
		return null;
	}

}
